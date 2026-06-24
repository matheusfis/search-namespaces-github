import { writeFile } from "node:fs/promises";
import { ConfigError, loadConfig } from "./config";
import { createOctokit } from "./github";
import { scanOrg } from "./scanner";
import { buildReport } from "./report";
import { errorMessage, httpStatus } from "./http-error";

const REPORT_FILE = "namespaces-report.md";

async function main(): Promise<void> {
  const config = loadConfig();

  // Diagnostico em stderr para manter o stdout limpo (apenas o relatorio).
  console.error(
    `Varredura -> org="${config.org}" | topics(AND)=[${config.topics.join(", ")}] | ` +
      `var="${config.varName}" | api=${config.apiUrl}`,
  );

  const octokit = createOctokit(config);
  const result = await scanOrg(octokit, config);

  // Relatorio em Markdown: impresso no console e gravado em arquivo.
  const report = buildReport(result);
  console.log(report);
  await writeFile(REPORT_FILE, report, "utf8");

  // Resumo de uma linha (stderr).
  console.error(
    `\nResumo: ${result.totalOrgRepos} repo(s) ativo(s) na org | ` +
      `${result.analyzedRepos} com todos os topics | ` +
      `${result.reposWithVar} com ${config.varName} | ` +
      `${result.namespaces.size} namespace(s) unico(s) | ` +
      `${result.reposWithoutVar.length} sem variavel | ` +
      `${result.errors.length} erro(s). Relatorio: ${REPORT_FILE}`,
  );
}

main().catch((err: unknown) => {
  // Configuracao invalida/ausente.
  if (err instanceof ConfigError) {
    console.error(`\n[Configuracao] ${err.message}`);
    process.exit(2);
  }

  // Erros HTTP globais (ocorrem ao listar repos da org).
  const status = httpStatus(err);
  if (status === 401) {
    console.error(
      "\n[Auth] Token ausente ou invalido (401). Verifique GITHUB_TOKEN e suas permissoes/SSO.",
    );
    process.exit(3);
  }
  if (status === 404) {
    console.error(
      "\n[GitHub] Recurso nao encontrado (404). Verifique se a organizacao existe e se o token tem acesso a ela.",
    );
    process.exit(4);
  }
  if (status === 403) {
    console.error(
      "\n[GitHub] Acesso negado (403). Verifique as permissoes/SSO do token ou aguarde o rate limit.",
    );
    process.exit(5);
  }

  console.error(`\n[Erro inesperado] ${errorMessage(err)}`);
  process.exit(1);
});
