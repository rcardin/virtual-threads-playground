package in.rcard.virtual.threads;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class GitHubApp {
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
      delay(Duration.ofMillis(500L));
      return new User(userId, new UserName("rcardin"), new Email("rcardin@rockthejvm.com"));
    }

    @Override
    public List<Repository> findRepositories(UserId userId) throws InterruptedException {
      delay(Duration.ofSeconds(1L));
      return List.of(
          new Repository(
              "raise4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/raise4s")),
          new Repository(
              "sus4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/sus4s")));
    }
  }

  private static void delay(Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }

  interface FindGitHubUserUseCase {
    GitHubUser findGitHubUser(UserId userId) throws InterruptedException;
  }

  static class FindGitHubUserSequentialService {
    private final FindUserByIdPort findUserByIdPort;
    private final FindRepositoriesByUserIdPort findRepositoriesByUserIdPort;

    public FindGitHubUserSequentialService(
        FindUserByIdPort findUserByIdPort,
        FindRepositoriesByUserIdPort findRepositoriesByUserIdPort) {
      this.findUserByIdPort = findUserByIdPort;
      this.findRepositoriesByUserIdPort = findRepositoriesByUserIdPort;
    }

    public GitHubUser findGitHubUser(UserId userId) throws InterruptedException {
      var user = findUserByIdPort.findUser(userId);
      var repositories = findRepositoriesByUserIdPort.findRepositories(userId);
      return new GitHubUser(user, repositories);
    }
  }

  static class FindGitHubUserConcurrentService {
    private final FindUserByIdPort findUserByIdPort;
    private final FindRepositoriesByUserIdPort findRepositoriesByUserIdPort;

    public FindGitHubUserConcurrentService(
        FindUserByIdPort findUserByIdPort,
        FindRepositoriesByUserIdPort findRepositoriesByUserIdPort) {
      this.findUserByIdPort = findUserByIdPort;
      this.findRepositoriesByUserIdPort = findRepositoriesByUserIdPort;
    }

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
}
