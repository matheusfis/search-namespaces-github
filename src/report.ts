import type { ScanResult } from "./scanner";

// Ordenacao estavel e amigavel para pt-BR.
const byName = (a: string, b: string): number => a.localeCompare(b, "pt-BR");

/**
 * Monta o relatorio em Markdown a partir do resultado da varredura.
 * Cada namespace aparece UMA unica vez, com seus repos ordenados.
 */
export function buildReport(result: ScanResult): string {
  const uniqueNamespaces = result.namespaces.size;
  const lines: string[] = [];

  // ---- Cabecalho ----
  lines.push("# Namespaces encontrados");
  lines.push("");
  lines.push(`Org: ${result.org} | Topics (AND): ${result.topics.join(", ")}`);
  lines.push(
    `Repos analisados: ${result.analyzedRepos} | ` +
      `Com ${result.varName}: ${result.reposWithVar} | ` +
      `Namespaces unicos: ${uniqueNamespaces}`,
  );
  lines.push("");

  // ---- Caso nenhum repo passe no filtro de topics ----
  if (result.analyzedRepos === 0) {
    lines.push(
      `> Nenhum repositorio da org **${result.org}** possui todos os topics ` +
        `informados (${result.topics.join(", ")}).`,
    );
    lines.push("");
    return lines.join("\n");
  }

  // ---- Uma secao por namespace (ordenados alfabeticamente) ----
  const sortedNamespaces = [...result.namespaces.keys()].sort(byName);

  if (sortedNamespaces.length === 0) {
    lines.push(
      `> Repos selecionados pelos topics nao possuem a variavel ${result.varName}.`,
    );
    lines.push("");
  }

  for (const namespace of sortedNamespaces) {
    const repos = [...result.namespaces.get(namespace)!].sort(byName);
    lines.push(`## ${namespace}`);
    for (const repo of repos) lines.push(`- ${repo}`);
    lines.push("");
  }

  // ---- Repos que passaram no filtro mas nao tinham a variavel ----
  if (result.reposWithoutVar.length > 0) {
    const repos = [...result.reposWithoutVar].sort(byName);
    lines.push(`## Repos sem a variavel ${result.varName}`);
    for (const repo of repos) lines.push(`- ${repo}`);
    lines.push("");
  }

  // ---- Repos pulados por erro de acesso (transparencia) ----
  if (result.errors.length > 0) {
    const errors = [...result.errors].sort((a, b) => byName(a.repo, b.repo));
    lines.push("## Repos com erro de acesso (ignorados)");
    for (const { repo, reason } of errors) lines.push(`- ${repo}: ${reason}`);
    lines.push("");
  }

  return lines.join("\n");
}
