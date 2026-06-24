import { Octokit } from "@octokit/rest";
import { throttling } from "@octokit/plugin-throttling";
import type { AppConfig } from "./config";

// `@octokit/rest` ja inclui o plugin de paginacao (octokit.paginate).
// Aqui adicionamos o plugin de throttling para tratar rate limit
// automaticamente.
const ThrottledOctokit = Octokit.plugin(throttling);

/** Tipo da instancia ja com os plugins aplicados. */
export type AppOctokit = InstanceType<typeof ThrottledOctokit>;

// Quantas vezes re-tentar uma requisicao bloqueada por rate limit.
const MAX_RETRIES = 2;

/**
 * Cria a instancia do Octokit apontando para a base da API informada
 * (api.github.com ou GitHub Enterprise Server `/api/v3`) com tratamento
 * automatico de rate limit primario e secundario.
 */
export function createOctokit(config: AppConfig): AppOctokit {
  return new ThrottledOctokit({
    auth: config.token,
    baseUrl: config.apiUrl,
    throttle: {
      // Rate limit primario (limite por hora). Aguarda `retryAfter` e re-tenta.
      onRateLimit: (retryAfter, options, octokit, retryCount) => {
        octokit.log.warn(
          `Rate limit atingido em ${options.method} ${options.url} ` +
            `- aguardando ${retryAfter}s.`,
        );
        if (retryCount < MAX_RETRIES) {
          octokit.log.info(`Re-tentativa ${retryCount + 1}/${MAX_RETRIES}.`);
          return true; // aguarda e re-tenta
        }
        octokit.log.warn("Limite de re-tentativas excedido; desistindo desta requisicao.");
        return false;
      },
      // Rate limit secundario (abuso/concorrencia). Mesmo comportamento.
      onSecondaryRateLimit: (retryAfter, options, octokit, retryCount) => {
        octokit.log.warn(
          `Secondary rate limit em ${options.method} ${options.url} ` +
            `- aguardando ${retryAfter}s.`,
        );
        if (retryCount < MAX_RETRIES) {
          octokit.log.info(`Re-tentativa ${retryCount + 1}/${MAX_RETRIES}.`);
          return true;
        }
        return false;
      },
    },
  });
}
