package githubnamespacescanner.domain.model;

import java.util.Objects;

public record ScannerSettings(
        ScanCriteria criteria,
        GitHubConnectionSettings connection
) {

    public ScannerSettings {
        criteria = Objects.requireNonNull(criteria, "Scan criteria must not be null");
        connection = Objects.requireNonNull(connection, "Connection settings must not be null");
    }
}
