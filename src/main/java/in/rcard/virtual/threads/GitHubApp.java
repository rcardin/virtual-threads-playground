package in.rcard.virtual.threads;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubApp {

  private static final Logger LOGGER = LoggerFactory.getLogger("GitHubApp");

  record GitHubUser(User user, List<Repository> repositories) {}

  record User(UserId userId, UserName name, Email email) {}

  record UserId(long value) {}

  record UserName(String value) {}

  record Email(String value) {}

  record Repository(String name, Visibility visibility, URI uri) {}

  enum Visibility {
    PUBLIC,
    PRIVATE
  }

  interface FindUserByIdPort {
    User findUser(UserId userId) throws InterruptedException;
  }

  interface FindRepositoriesByUserIdPort {
    List<Repository> findRepositories(UserId userId) throws InterruptedException;
  }

  static class GitHubRepository implements FindUserByIdPort, FindRepositoriesByUserIdPort {

    @Override
    public User findUser(UserId userId) throws InterruptedException {
      LOGGER.info("Finding user with id '{}'", userId);
      delay(Duration.ofMillis(500L));
      LOGGER.info("User '{}' found", userId);
      return new User(userId, new UserName("rcardin"), new Email("rcardin@rockthejvm.com"));
    }

    @Override
    public List<Repository> findRepositories(UserId userId) throws InterruptedException {
      LOGGER.info("Finding repositories for user with id '{}'", userId);
      delay(Duration.ofSeconds(1L));
      LOGGER.info("Repositories found for user '{}'", userId);
      return List.of(
          new Repository(
              "raise4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/raise4s")),
          new Repository(
              "sus4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/sus4s")));
    }

//    @Override
//    public User findUser(UserId userId) throws InterruptedException {
//      LOGGER.info("Finding user with id '{}'", userId);
//      delay(Duration.ofMillis(100L));
//      throw new RuntimeException("Socket timeout");
//    }
  }

  private static void delay(Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }

  interface FindGitHubUserUseCase {
    GitHubUser findGitHubUser(UserId userId) throws InterruptedException, ExecutionException;
  }

  static class FindGitHubUserSequentialService implements FindGitHubUserUseCase {
    private final FindUserByIdPort findUserByIdPort;
    private final FindRepositoriesByUserIdPort findRepositoriesByUserIdPort;

    public FindGitHubUserSequentialService(
        FindUserByIdPort findUserByIdPort,
        FindRepositoriesByUserIdPort findRepositoriesByUserIdPort) {
      this.findUserByIdPort = findUserByIdPort;
      this.findRepositoriesByUserIdPort = findRepositoriesByUserIdPort;
    }

    @Override
    public GitHubUser findGitHubUser(UserId userId) throws InterruptedException {
      var user = findUserByIdPort.findUser(userId);
      var repositories = findRepositoriesByUserIdPort.findRepositories(userId);
      return new GitHubUser(user, repositories);
    }
  }

  static class FindGitHubUserConcurrentService implements FindGitHubUserUseCase {
    private final FindUserByIdPort findUserByIdPort;
    private final FindRepositoriesByUserIdPort findRepositoriesByUserIdPort;

    public FindGitHubUserConcurrentService(
        FindUserByIdPort findUserByIdPort,
        FindRepositoriesByUserIdPort findRepositoriesByUserIdPort) {
      this.findUserByIdPort = findUserByIdPort;
      this.findRepositoriesByUserIdPort = findRepositoriesByUserIdPort;
    }

    //    @Override
    //    public GitHubUser findGitHubUser(UserId userId) throws InterruptedException,
    // ExecutionException {
    //      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    //        var user = executor.submit(() -> findUserByIdPort.findUser(userId));
    //        var repositories =
    //                executor.submit(() -> findRepositoriesByUserIdPort.findRepositories(userId));
    //        throw new RuntimeException("Something went wrong");
    //      }
    //    }

    @Override
    public GitHubUser findGitHubUser(UserId userId)
        throws InterruptedException, ExecutionException {
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var user = executor.submit(() -> findUserByIdPort.findUser(userId));
        var repositories =
            executor.submit(() -> findRepositoriesByUserIdPort.findRepositories(userId));
        return new GitHubUser(user.get(), repositories.get());
      }
    }
  }

  public static void main() throws ExecutionException, InterruptedException {
    final GitHubRepository gitHubRepository = new GitHubRepository();
    FindGitHubUserUseCase service =
        new FindGitHubUserConcurrentService(gitHubRepository, gitHubRepository);

    final GitHubUser gitHubUser = service.findGitHubUser(new UserId(1L));
    LOGGER.info("GitHub user: {}", gitHubUser);
  }
}
