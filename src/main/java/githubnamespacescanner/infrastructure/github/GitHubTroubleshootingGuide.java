package githubnamespacescanner.infrastructure.github;

final class GitHubTroubleshootingGuide {

    static final String AUTHENTICATION = """
            1) verifique se GITHUB_TOKEN está definido, válido e não expirou; \
            2) confirme os scopes repo e read:org em https://github.com/settings/tokens \
            (ou Metadata: Read e Variables: Read em token fine-grained); \
            3) se usar GitHub App, gere um novo Installation Token — ele expira em 1 hora; \
            4) se a organização exige SSO/SAML, autorize o token para essa organização.""";

    static final String NOT_FOUND = """
            1) confirme que o repositório/organização existe e não foi renomeado ou deletado; \
            2) verifique se o token tem acesso ao repositório — repositório privado requer \
            acesso repo, não apenas public_repo; \
            3) para Actions Variables, confirme o nome exato e case-sensitive da variável; \
            4) confira se o endpoint é repo-level ou org-level; \
            5) em organizações com SSO/SAML, confirme a autorização do token.""";

    static final String RATE_LIMIT = """
            aguarde o instante indicado por X-RateLimit-Reset antes de tentar novamente; \
            reduza chamadas concorrentes e consulte os headers X-RateLimit-* da resposta.""";

    static final String FORBIDDEN = """
            confirme as permissões do token, a autorização SSO/SAML e as políticas da organização; \
            se X-GitHub-SSO estiver presente, use a URL informada nesse header.""";

    static final String UNAVAILABLE = """
            tente novamente mais tarde, consulte https://www.githubstatus.com e, se não houve \
            resposta HTTP, valide rede, PROXY_URL, NO_PROXY e GITHUB_API_CERT_PATH.""";

    static final String GENERIC = """
            revise a resposta da GitHub API, as permissões do token e a versão do endpoint.""";

    private GitHubTroubleshootingGuide() {
    }
}
