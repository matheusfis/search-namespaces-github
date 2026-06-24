# GitHub Namespace Scanner

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)

CLI em Java 21 e Spring Boot que:

1. lista todos os repositórios visíveis de uma organização do GitHub;
2. ignora repositórios arquivados ou desabilitados;
3. seleciona apenas os repositórios que contêm **todos** os tópicos configurados
   (lógica AND, case-insensitive);
4. consulta uma Actions Repository Variable específica, `NAMESPACE` por padrão;
5. agrupa e deduplica os repositórios pelo valor dessa variável;
6. imprime e grava o relatório em `namespaces-report.md`.

O valor da variável recebe `trim()`, mas sua caixa é preservada. Uma variável
ausente ou vazia é registrada como “sem variável”. Falhas pontuais de acesso
são incluídas no relatório sem interromper os demais repositórios.

## Pré-requisitos

- JDK 21;
- Maven 3.6.3 ou superior;
- token do GitHub com acesso aos repositórios consultados.

O projeto usa Maven porque ele oferece integração direta e previsível com o
Spring Boot, JUnit, JaCoCo, CI e gerenciamento de dependências em um único
`pom.xml`.

## Configuração

Copie o exemplo:

```powershell
Copy-Item .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

Variáveis disponíveis:

| Variável | Obrigatória | Padrão | Descrição |
| --- | --- | --- | --- |
| `GITHUB_TOKEN` | Sim | — | Token enviado como `Bearer`. Para PAT classic, use `repo` e `read:org` quando aplicável. |
| `GITHUB_ORG` | Sim | — | Organização consultada; pode ser sobrescrita por `--org`. |
| `TOPICS` | Sim | — | Tópicos separados por vírgula. Todos devem existir no repositório. |
| `VAR_NAME` | Não | `NAMESPACE` | Nome exato e case-sensitive da Actions Repository Variable. |
| `GITHUB_API_URL` | Não | `https://api.github.com` | Base da API; para GitHub Enterprise Server, use `https://host/api/v3`. |
| `PROXY_URL` | Não | vazio | Proxy HTTP/HTTPS, por exemplo `http://proxy.empresa.com:8080`. |
| `NO_PROXY` | Não | vazio | Hosts ignorados pelo proxy, separados por vírgula. Aceita host exato, `.dominio`, `*.dominio` e `*`. |
| `GITHUB_API_CERT_PATH` | Não | vazio | Arquivo CA X.509 em PEM ou DER, adicionado às CAs já confiadas pela JVM. |

Para token fine-grained, conceda no mínimo:

- Repository permissions → **Metadata: Read**;
- Repository permissions → **Variables: Read**;
- acesso aos repositórios e à organização necessários.

O arquivo `.env` está no `.gitignore`. Nunca grave tokens reais em arquivos
rastreados.

## Execução

Com o `.env`:

```bash
mvn spring-boot:run
```

Sobrescrevendo organização e tópicos:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--org minha-org --topics backstage,openshift"
```

Também são aceitas as formas `--org=minha-org` e
`--topics=backstage,openshift`. Opções desconhecidas são ignoradas para manter
compatibilidade com a CLI anterior.

Para gerar e executar o JAR:

```bash
mvn clean package
java -jar target/github-namespace-scanner-2.0.0-SNAPSHOT.jar
```

## Testes e cobertura

```bash
mvn clean verify
```

O build exige pelo menos 80% de cobertura de linhas nas camadas de domínio e
aplicação. O relatório HTML é gerado em:

```text
target/site/jacoco/index.html
```

Resultado da refatoração:

- 37 testes;
- domínio: 97,31% das linhas;
- aplicação: 98,28% das linhas;
- domínio + aplicação: 97,68% das linhas.

## Decisões de arquitetura

### Domain-Driven Design

- `domain`: entidades, records imutáveis, invariantes, resultados, exceções e
  a porta `GitHubRepositoryPort`;
- `application`: `ScanRepositoriesUseCase` e formatação do relatório;
- `infrastructure`: cliente REST do GitHub, `.env`, proxy, TLS e certificado;
- `presentation`: runner CLI e códigos de saída.

O entry point continua sendo CLI porque esse era o comportamento da aplicação
original. Não foram adicionados controller REST, scheduler ou operações de
escrita no GitHub.

### Busca dos repositórios

Foi escolhido:

```text
GET /orgs/{org}/repos?type=all&per_page=100&page={n}
```

em vez de `GET /search/repositories`, GraphQL ou busca por prefixo de tópico.
Essa decisão preserva a regra confirmada da aplicação original:

- o escopo é uma organização específica;
- o filtro recebe vários tópicos configuráveis com lógica AND;
- repositórios privados visíveis ao token devem participar;
- todos os repositórios ativos precisam ser contados;
- a Search API possui rate limit mais restritivo e limite próprio de
  resultados;
- GraphQL não elimina a chamada REST necessária para Actions Variables.

A paginação usa o máximo de 100 itens por página e continua até a última
página. Os tópicos retornados na listagem são usados diretamente; se vierem
vazios, há fallback para:

```text
GET /repos/{owner}/{repo}/topics
```

### Leitura da variável

Foi escolhido o endpoint específico:

```text
GET /repos/{owner}/{repo}/actions/variables/{name}
```

Ele evita baixar todas as variáveis quando apenas uma é necessária. A API
GraphQL não oferece operação equivalente para Actions Repository Variables.
Um `404` nesse endpoint mantém o comportamento anterior e representa variável
ausente.

Referências oficiais:

- [List organization repositories](https://docs.github.com/en/rest/repos/repos#list-organization-repositories)
- [Get a repository variable](https://docs.github.com/en/rest/actions/variables#get-a-repository-variable)
- [Rate limits for the REST API](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api)

## Proxy e certificado corporativo

Quando `PROXY_URL` está vazio ou ausente, o cliente usa conexão direta. Quando
está preenchido, o `HttpClient` do Java 21 encaminha HTTP e HTTPS pelo proxy,
exceto para hosts correspondentes a `NO_PROXY`.

Se `GITHUB_API_CERT_PATH` estiver preenchido, os certificados X.509 do arquivo
são combinados com o trust store padrão da JVM. Caminho inexistente, arquivo
ilegível ou conteúdo que não seja certificado gera erro de configuração
acionável, sem `NullPointerException`.

## Tratamento de erros

A hierarquia de domínio é:

```text
GitHubApiException
├── GitHubAuthenticationException       (401)
├── GitHubNotFoundException             (404)
├── GitHubRateLimitException            (403 e X-RateLimit-Remaining: 0)
└── GitHubApiUnavailableException       (5xx ou falha de conexão)
```

Todo diagnóstico inclui:

- endpoint chamado;
- status HTTP recebido (`0` quando não houve resposta HTTP);
- `X-GitHub-Request-Id`, quando fornecido;
- guia de troubleshooting.

### 401 Unauthorized

1. Verifique se `GITHUB_TOKEN` está definido, válido e não expirou.
2. Confirme os scopes `repo` e `read:org` em
   <https://github.com/settings/tokens>.
3. Para token fine-grained, confira **Metadata: Read**, **Variables: Read** e
   os repositórios selecionados.
4. Se usar GitHub App, gere um novo Installation Token; ele expira após uma
   hora.
5. Se a organização exige SSO/SAML, autorize o token para a organização.

### 404 Not Found

1. Confirme que organização e repositório existem e não foram renomeados ou
   deletados.
2. Confirme que o token tem acesso ao repositório. Repositório privado requer
   acesso `repo`, não apenas `public_repo`.
3. Para Actions Variables, confirme o nome exato e case-sensitive.
4. Confira se o recurso é repository-level ou organization-level.
5. Verifique a autorização SSO/SAML: o GitHub pode responder `404` para não
   revelar recursos aos quais o token não tem acesso.

Consulte também:

- [Troubleshooting the REST API](https://docs.github.com/en/rest/using-the-rest-api/troubleshooting-the-rest-api)
- [Authenticating to the REST API](https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api)
- [Authorizing a PAT for SSO](https://docs.github.com/en/authentication/authenticating-with-saml-single-sign-on/authorizing-a-personal-access-token-for-use-with-saml-single-sign-on)

## Códigos de saída

| Código | Significado |
| --- | --- |
| `0` | Varredura concluída. |
| `1` | Erro inesperado ou indisponibilidade da API. |
| `2` | Configuração ausente ou inválida. |
| `3` | Autenticação inválida (`401`). |
| `4` | Organização/recurso global não encontrado (`404`). |
| `5` | Acesso negado ou rate limit (`403`). |

## Segurança e licença

Consulte [SECURITY.md](SECURITY.md) para reportar vulnerabilidades. O projeto é
distribuído sob a licença MIT; consulte [LICENSE](LICENSE).
