package in.rcard.virtual.threads;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("preview")
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
    List<Repository> findRepositories(UserId userId)
        throws InterruptedException, ExecutionException;
  }

  static class FindRepositoriesByUserIdCache implements FindRepositoriesByUserIdPort {

    private final Map<UserId, List<Repository>> cache = new HashMap<>();

    public FindRepositoriesByUserIdCache() {
      cache.put(
          new UserId(42L),
          List.of(
              new Repository(
                  "rockthejvm.github.io",
                  Visibility.PUBLIC,
                  URI.create("https://github.com/rockthejvm/rockthejvm.github.io"))));
    }

    @Override
    public List<Repository> findRepositories(UserId userId) throws InterruptedException {
      // Simulates access to a distributed cache (Redis?)
      delay(Duration.ofMillis(100L));
      final List<Repository> repositories = cache.get(userId);
      if (repositories == null) {
        LOGGER.info("No cached repositories found for user with id '{}'", userId);
        throw new NoSuchElementException(
            "No cached repositories found for user with id '%s'".formatted(userId));
      }
      return repositories;
    }

    public void addToCache(UserId userId, List<Repository> repositories)
        throws InterruptedException {
      // Simulates access to a distributed cache (Redis?)
      delay(Duration.ofMillis(100L));
      cache.put(userId, repositories);
    }
  }

  static class GitHubCachedRepository implements FindRepositoriesByUserIdPort {

    private final FindRepositoriesByUserIdPort repository;
    private final FindRepositoriesByUserIdCache cache;

    GitHubCachedRepository(
        FindRepositoriesByUserIdPort repository, FindRepositoriesByUserIdCache cache) {
      this.repository = repository;
      this.cache = cache;
    }

    @Override
    public List<Repository> findRepositories(UserId userId)
        throws InterruptedException, ExecutionException {
      try (var scope = new StructuredTaskScope.ShutdownOnSuccess<List<Repository>>()) {
        scope.fork(() -> cache.findRepositories(userId));
        scope.fork(
            () -> {
              final List<Repository> repositories = repository.findRepositories(userId);
              cache.addToCache(userId, repositories);
              return repositories;
            });
        return scope.join().result();
      }
    }
  }

  static class GitHubRepository implements FindUserByIdPort, FindRepositoriesByUserIdPort {

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

    //        @Override
    //        public User findUser(UserId userId) throws InterruptedException {
    //          LOGGER.info("Finding user with id '{}'", userId);
    //          delay(Duration.ofMillis(500L));
    //          LOGGER.info("User '{}' found", userId);
    //          return new User(userId, new UserName("rcardin"), new
    // Email("rcardin@rockthejvm.com"));
    //        }

    @Override
    public User findUser(UserId userId) throws InterruptedException {
      LOGGER.info("Finding user with id '{}'", userId);
      delay(Duration.ofMillis(100L));
      throw new RuntimeException("Socket timeout");
    }
  }

  private static void delay(Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }

  interface FindGitHubUserUseCase {
    GitHubUser findGitHubUser(UserId userId) throws Throwable;
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
    public GitHubUser findGitHubUser(UserId userId)
        throws InterruptedException, ExecutionException {
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

  static class FindGitHubUserStructuredConcurrencyService implements FindGitHubUserUseCase {

    private final FindUserByIdPort findUserByIdPort;
    private final FindRepositoriesByUserIdPort findRepositoriesByUserIdPort;

    FindGitHubUserStructuredConcurrencyService(
        FindUserByIdPort findUserByIdPort,
        FindRepositoriesByUserIdPort findRepositoriesByUserIdPort) {
      this.findUserByIdPort = findUserByIdPort;
      this.findRepositoriesByUserIdPort = findRepositoriesByUserIdPort;
    }

    @Override
    public GitHubUser findGitHubUser(UserId userId)
        throws ExecutionException, InterruptedException {

      var result =
          par(
              () -> findUserByIdPort.findUser(userId),
              () -> findRepositoriesByUserIdPort.findRepositories(userId));
      return new GitHubUser(result.first(), result.second());

      //      try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      //        var user = scope.fork(() -> findUserByIdPort.findUser(userId));
      //        var repositories = scope.fork(() ->
      // findRepositoriesByUserIdPort.findRepositories(userId));
      //
      //        LOGGER.info("Both forked task completed");
      //
      //        scope.join().throwIfFailed(Function.identity());
      //
      //        return new GitHubUser(user.get(), repositories.get());
      //      }
    }
  }

  public record Pair<T1, T2>(T1 first, T2 second) {}

  public static <T1, T2> Pair<T1, T2> par(Callable<T1> first, Callable<T2> second)
      throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var firstTask = scope.fork(first);
      var secondTask = scope.fork(second);
      scope.join().throwIfFailed();
      return new Pair<>(firstTask.get(), secondTask.get());
    }
  }

  public static void main() throws ExecutionException, InterruptedException {
    final GitHubRepository gitHubRepository = new GitHubRepository();
    final FindRepositoriesByUserIdCache cache = new FindRepositoriesByUserIdCache();
    final FindRepositoriesByUserIdPort gitHubCachedRepository =
        new GitHubCachedRepository(gitHubRepository, cache);

    final List<Repository> repositories = gitHubCachedRepository.findRepositories(new UserId(42L));

    LOGGER.info("GitHub user's repositories: {}", repositories);
  }
}
