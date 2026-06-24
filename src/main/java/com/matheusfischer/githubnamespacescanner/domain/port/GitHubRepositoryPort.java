package com.matheusfischer.githubnamespacescanner.domain.port;

import com.matheusfischer.githubnamespacescanner.domain.model.GitHubRepository;
import com.matheusfischer.githubnamespacescanner.domain.model.OrganizationName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryTopic;
import com.matheusfischer.githubnamespacescanner.domain.model.VariableLookupResult;
import com.matheusfischer.githubnamespacescanner.domain.model.VariableName;

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
