import type { AppConfig } from "./config";
import type { AppOctokit } from "./github";
import { errorMessage, httpStatus } from "./http-error";

/** Resultado consolidado da varredura. */
export interface ScanResult {
  org: string;
  topics: string[];
  varName: string;
  /** Repos ativos encontrados na org (apos ignorar archived/disabled). */
  totalOrgRepos: number;
  /** Repos que passaram no filtro de topics (AND) -> "repos analisados". */
  analyzedRepos: number;
  /** Repos analisados que possuiam a variavel com valor nao vazio. */
  reposWithVar: number;
  /** namespace (com trim) -> conjunto de repos (Set evita duplicatas). */
  namespaces: Map<string, Set<string>>;
  /** Repos que passaram no filtro mas nao tinham a variavel (ou valor vazio). */
  reposWithoutVar: string[];
  /** Repos pulados por erro de acesso (403, etc.). */
  errors: { repo: string; reason: string }[];
}

/**
 * Resolve os topics de um repo. O array `topics` ja costuma vir na listagem;
 * caso venha vazio/ausente, usa o fallback `repos.getAllTopics`.
 *
 * Observacao de custo: um repo realmente sem topics nunca passa no filtro AND,
 * entao o fallback so e util quando a listagem nao popula `topics` mas o repo
 * de fato possui algum (cenario raro na API atual). Mantido conforme requisito.
 */
async function resolveTopics(
  octokit: AppOctokit,
  owner: string,
  repo: string,
  inlineTopics: string[] | undefined,
): Promise<string[]> {
  if (inlineTopics && inlineTopics.length > 0) return inlineTopics;
  try {
    const { data } = await octokit.rest.repos.getAllTopics({ owner, repo });
    return data.names ?? [];
  } catch {
    // Fallback falhou: trata como "sem topics" (nao interrompe a varredura).
    return inlineTopics ?? [];
  }
}

/**
 * Varre todos os repos da org, filtra por topics (AND), le a variavel
 * `config.varName` de cada selecionado e agrupa por namespace.
 */
export async function scanOrg(
  octokit: AppOctokit,
  config: AppConfig,
): Promise<ScanResult> {
  // Comparacao case-insensitive: topics do GitHub sao sempre minusculos.
  const wantedTopics = config.topics.map((topic) => topic.toLowerCase());

  // 1. Lista TODOS os repos da org (paginacao automatica, 100 por pagina).
  //    Erros globais (401/404/403 na org) propagam para o entrypoint.
  const allRepos = await octokit.paginate(octokit.rest.repos.listForOrg, {
    org: config.org,
    per_page: 100,
    type: "all",
  });

  // 2. Ignora repos arquivados e desabilitados.
  const activeRepos = allRepos.filter((repo) => !repo.archived && !repo.disabled);

  const result: ScanResult = {
    org: config.org,
    topics: config.topics,
    varName: config.varName,
    totalOrgRepos: activeRepos.length,
    analyzedRepos: 0,
    reposWithVar: 0,
    namespaces: new Map(),
    reposWithoutVar: [],
    errors: [],
  };

  for (const repo of activeRepos) {
    // 3. Normaliza topics (com fallback).
    const topics = await resolveTopics(octokit, config.org, repo.name, repo.topics);
    const normalized = topics.map((topic) => topic.toLowerCase());

    // 4. Filtro AND: o repo precisa conter TODOS os topics informados.
    const matchesAll = wantedTopics.every((topic) => normalized.includes(topic));
    if (!matchesAll) continue;

    result.analyzedRepos++;

    // 5. Le a variavel pelo nome conhecido (mais barato que listar todas).
    try {
      const { data } = await octokit.rest.actions.getRepoVariable({
        owner: config.org,
        repo: repo.name,
        name: config.varName,
      });

      // 6. Normaliza o valor com trim() (para deduplicar valores com espacos),
      //    sem alterar a caixa do conteudo.
      const namespace = data.value.trim();
      if (namespace.length === 0) {
        // Variavel existe mas esta vazia: tratamos como "sem namespace".
        result.reposWithoutVar.push(repo.name);
        continue;
      }

      // 7. Agrega em Map<string, Set<string>> (Set evita repo duplicado).
      let bucket = result.namespaces.get(namespace);
      if (!bucket) {
        bucket = new Set<string>();
        result.namespaces.set(namespace, bucket);
      }
      bucket.add(repo.name);
      result.reposWithVar++;
    } catch (err) {
      const status = httpStatus(err);
      if (status === 404) {
        // 404 = repo nao possui a variavel. NUNCA e erro fatal.
        result.reposWithoutVar.push(repo.name);
      } else if (status === 403) {
        // Sem permissao neste repo especifico: loga e segue.
        result.errors.push({
          repo: repo.name,
          reason: `403 (acesso negado): ${errorMessage(err)}`,
        });
      } else {
        // Outros erros pontuais: registra e segue para o proximo repo.
        result.errors.push({
          repo: repo.name,
          reason: status ? `${status}: ${errorMessage(err)}` : errorMessage(err),
        });
      }
    }
  }

  return result;
}
