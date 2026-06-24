package githubnamespacescanner.domain.port;

import githubnamespacescanner.domain.model.GitHubRepository;
import githubnamespacescanner.domain.model.OrganizationName;
import githubnamespacescanner.domain.model.RepositoryName;
import githubnamespacescanner.domain.model.RepositoryTopic;
import githubnamespacescanner.domain.model.VariableLookupResult;
import githubnamespacescanner.domain.model.VariableName;

import java.util.List;

public interface GitHubRepositoryPort {

    List<GitHubRepository> findAllByOrganization(OrganizationName organization);

    List<RepositoryTopic> findTopics(OrganizationName organization, RepositoryName repository);

    VariableLookupResult findVariable(
            OrganizationName organization,
            RepositoryName repository,
            VariableName variableName
    );
}
