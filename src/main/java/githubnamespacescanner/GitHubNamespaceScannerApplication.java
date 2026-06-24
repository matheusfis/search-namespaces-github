package githubnamespacescanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GitHubNamespaceScannerApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(GitHubNamespaceScannerApplication.class, args);
        var exitCode = SpringApplication.exit(context);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
