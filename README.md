# GitHub Namespace Scanner

CLI em TypeScript que procura repositórios de uma organização do GitHub por
topics e agrupa os resultados pelo valor de uma Actions Repository Variable.

Por padrão, o projeto procura a variável `NAMESPACE`, exige que todos os topics
informados estejam presentes e grava o resultado em `namespaces-report.md`.

## Requisitos

- Node.js 20 ou superior
- npm
- Um token do GitHub com o menor conjunto de permissões necessário:
  - PAT fine-grained: `Variables: Read` e `Metadata: Read` nos repositórios
    consultados; ou
  - PAT classic: escopo `repo` quando for necessário acessar repositórios
    privados.

Nunca salve tokens no repositório. O arquivo `.env` é ignorado pelo Git.

## Instalação

```bash
npm ci
```

Crie a configuração local a partir do exemplo:

```powershell
Copy-Item .env.example .env
```

Em Linux ou macOS:

```bash
cp .env.example .env
```

## Configuração

| Variável | Obrigatória | Padrão | Descrição |
| --- | --- | --- | --- |
| `GITHUB_TOKEN` | Sim | — | Token usado apenas em memória para consultar a API. |
| `GITHUB_ORG` | Sim | — | Organização consultada; pode ser substituída por `--org`. |
| `TOPICS` | Sim | — | Topics separados por vírgula; o filtro usa lógica AND. |
| `GITHUB_API_URL` | Não | `https://api.github.com` | URL base da API, incluindo GitHub Enterprise. |
| `VAR_NAME` | Não | `NAMESPACE` | Nome da Actions Repository Variable consultada. |

## Uso

Com as opções definidas no `.env`:

```bash
npm start
```

Substituindo organização e topics pela linha de comando:

```bash
npm start -- --org minha-org --topics backstage,openshift
```

O relatório é exibido no terminal e salvo localmente em
`namespaces-report.md`. Esse arquivo é ignorado porque pode conter nomes de
repositórios e namespaces internos.

## Validação

```bash
npm run typecheck
npm audit
```

## Como funciona

1. Lista os repositórios da organização com paginação.
2. Ignora repositórios arquivados ou desabilitados.
3. Seleciona apenas os repositórios que possuem todos os topics informados.
4. Consulta a variável configurada em cada repositório selecionado.
5. Agrupa e deduplica os repositórios por namespace.
6. Registra separadamente repositórios sem a variável ou sem acesso.

## Códigos de saída

| Código | Significado |
| --- | --- |
| `0` | Execução concluída. |
| `1` | Erro inesperado. |
| `2` | Configuração ausente ou inválida. |
| `3` | Token ausente ou inválido. |
| `4` | Organização ou recurso não encontrado. |
| `5` | Acesso negado. |

## Segurança

Consulte [SECURITY.md](SECURITY.md) para reportar vulnerabilidades de forma
responsável. Não publique tokens, detalhes de organizações privadas ou
relatórios gerados em issues.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).
