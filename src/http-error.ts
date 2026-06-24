/**
 * Utilitarios para inspecionar erros HTTP do Octokit sem depender de
 * `instanceof RequestError`.
 *
 * Em projetos com Octokit pode haver mais de uma versao de
 * `@octokit/request-error` na arvore de dependencias; nesse caso o
 * `instanceof` falha silenciosamente. Ler a propriedade `status` de forma
 * defensiva e mais robusto e evita uma dependencia direta extra.
 */

/** Retorna o status HTTP do erro Octokit, ou `undefined` se nao houver. */
export function httpStatus(err: unknown): number | undefined {
  if (err && typeof err === "object" && "status" in err) {
    const status = (err as Record<string, unknown>).status;
    if (typeof status === "number") return status;
  }
  return undefined;
}

/** Extrai uma mensagem legivel de qualquer valor de erro. */
export function errorMessage(err: unknown): string {
  if (err && typeof err === "object" && "message" in err) {
    const message = (err as Record<string, unknown>).message;
    if (typeof message === "string") return message;
  }
  return String(err);
}
