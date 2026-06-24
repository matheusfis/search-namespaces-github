import { parseArgs } from "node:util";
import dotenv from "dotenv";

// Carrega variaveis do arquivo .env para process.env (no-op se nao existir).
dotenv.config();

/** Erro de configuracao (uso de exit code dedicado no entrypoint). */
export class ConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ConfigError";
  }
}

/** Configuracao efetiva da aplicacao, apos .env + overrides de CLI. */
export interface AppConfig {
  token: string;
  org: string;
  apiUrl: string;
  topics: string[];
  varName: string;
}

/** Converte "a, b ,c" -> ["a", "b", "c"], removendo vazios e espacos. */
function parseTopics(raw: string | undefined): string[] {
  if (!raw) return [];
  return raw
    .split(",")
    .map((topic) => topic.trim())
    .filter((topic) => topic.length > 0);
}

/**
 * Le a configuracao do ambiente (.env) e aplica overrides de linha de comando.
 *
 * Overrides aceitos:
 *   --org <org>             sobrescreve GITHUB_ORG
 *   --topics <t1,t2,...>    sobrescreve TOPICS
 *
 * Lanca {@link ConfigError} se faltar algum valor obrigatorio.
 */
export function loadConfig(argv: string[] = process.argv.slice(2)): AppConfig {
  // strict:false para nao quebrar caso o npm/usuario passe flags extras.
  const { values } = parseArgs({
    args: argv,
    options: {
      org: { type: "string" },
      topics: { type: "string" },
    },
    strict: false,
    allowPositionals: true,
  });

  const cliOrg = typeof values.org === "string" ? values.org : undefined;
  const cliTopics = typeof values.topics === "string" ? values.topics : undefined;

  const token = process.env.GITHUB_TOKEN?.trim();
  const org = (cliOrg ?? process.env.GITHUB_ORG)?.trim();
  const apiUrl = process.env.GITHUB_API_URL?.trim() || "https://api.github.com";
  const varName = process.env.VAR_NAME?.trim() || "NAMESPACE";
  const topics = parseTopics(cliTopics ?? process.env.TOPICS);

  // Validacao das entradas obrigatorias.
  const missing: string[] = [];
  if (!token) missing.push("GITHUB_TOKEN");
  if (!org) missing.push("GITHUB_ORG (ou --org)");
  if (topics.length === 0) missing.push("TOPICS (ou --topics)");

  if (missing.length > 0) {
    throw new ConfigError(
      `Configuracao obrigatoria ausente: ${missing.join(", ")}.\n` +
        `Preencha o arquivo .env (veja .env.example) ou informe via --org / --topics.`,
    );
  }

  // Apos a validacao, os valores obrigatorios estao garantidos.
  return {
    token: token as string,
    org: org as string,
    apiUrl,
    topics,
    varName,
  };
}
