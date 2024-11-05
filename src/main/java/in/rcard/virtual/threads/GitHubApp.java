package in.rcard.virtual.threads;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
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

  static class FindRepositoriesByUserIdWithTimeout {

    final FindRepositoriesByUserIdPort delegate;

    FindRepositoriesByUserIdWithTimeout(FindRepositoriesByUserIdPort delegate) {
      this.delegate = delegate;
    }

    List<Repository> findRepositories(UserId userId, Duration timeout)
        throws InterruptedException, ExecutionException {

      try (var scope = new ShutdownOnResult<List<Repository>>()) {
        scope.fork(() -> delegate.findRepositories(userId));
        scope.fork(
            () -> {
              delay(timeout);
              throw new TimeoutException("Timeout of %s reached".formatted(timeout));
            });
        return scope.join().resultOrThrow();
      }
    }
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

      return raceAll(
          () -> cache.findRepositories(userId),
          () -> {
            final List<Repository> repositories = repository.findRepositories(userId);
            cache.addToCache(userId, repositories);
            return repositories;
          });

      //      try (var scope = new StructuredTaskScope.ShutdownOnSuccess<List<Repository>>()) {
      //        scope.fork(() -> cache.findRepositories(userId));
      //        scope.fork(
      //            () -> {
      //              final List<Repository> repositories = repository.findRepositories(userId);
      //              cache.addToCache(userId, repositories);
      //              return repositories;
      //            });
      //        return scope.join().result();
      //      }
    }
  }

  static class GitHubRepository implements FindUserByIdPort, FindRepositoriesByUserIdPort {

    @Override
    public List<Repository> findRepositories(UserId userId) throws InterruptedException {
      LOGGER.info("Finding repositories for user with id '{}'", userId);
      delay(Duration.ofSeconds(1L));
      //      throw new RuntimeException("Socket timeout");
      LOGGER.info("Repositories found for user '{}'", userId);
      return List.of(
          new Repository(
              "raise4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/raise4s")),
          new Repository(
              "sus4s", Visibility.PUBLIC, URI.create("https://github.com/rcardin/sus4s")));
    }

    @Override
    public User findUser(UserId userId) throws InterruptedException {
      LOGGER.info("Finding user with id '{}'", userId);
      delay(Duration.ofMillis(500L));
      LOGGER.info("User '{}' found", userId);
      return new User(userId, new UserName("rcardin"), new Email("rcardin@rockthejvm.com"));
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
    GitHubUser findGitHubUser(UserId userId) throws Throwable;

    List<GitHubUser> findGitHubUsers(List<UserId> userIds, Duration timeout)
        throws InterruptedException, ExecutionException;
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

    @Override
    public List<GitHubUser> findGitHubUsers(List<UserId> userIds, Duration timeout) {
      return List.of();
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

    @Override
    public List<GitHubUser> findGitHubUsers(List<UserId> userIds, Duration timeout)
        throws InterruptedException, ExecutionException {
      return List.of();
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

      //      try (var scope = new StructuredTaskScope<>()) {
      //        var user = scope.fork(() -> findUserByIdPort.findUser(userId));
      //        var repositories = scope.fork(() ->
      // findRepositoriesByUserIdPort.findRepositories(userId));
      //
      //        LOGGER.info("Both forked task completed");
      //
      //        return new GitHubUser(user.get(), repositories.get());
      //      }
    }

    @Override
    public List<GitHubUser> findGitHubUsers(List<UserId> userIds, Duration timeout)
        throws InterruptedException, ExecutionException {

      return timeout(
          timeout,
          () ->
              par(
                  userIds.stream()
                      .map(
                          userId ->
                              (Callable<GitHubUser>)
                                  () -> {
                                    try {
                                      return findGitHubUser(userId);
                                    } catch (ExecutionException | InterruptedException e) {
                                      throw new RuntimeException(e);
                                    }
                                  })
                      .toList()));
    }
  }

  record Pair<T1, T2>(T1 first, T2 second) {}

  static <T1, T2> Pair<T1, T2> par(Callable<T1> first, Callable<T2> second)
      throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var firstTask = scope.fork(first);
      var secondTask = scope.fork(second);
      scope.join().throwIfFailed();
      return new Pair<>(firstTask.get(), secondTask.get());
    }
  }

  static <T> List<T> par(List<Callable<T>> tasks) throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var subtasks = tasks.stream().map(scope::fork).toList();
      scope.join().throwIfFailed();
      return subtasks.stream().map(Supplier::get).toList();
    }
  }

  static <T> T raceAll(Callable<T> first, Callable<T> second)
      throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnSuccess<T>()) {
      scope.fork(first);
      scope.fork(second);
      return scope.join().result();
    }
  }

  static class ShutdownOnResult<T> extends StructuredTaskScope<T> {

    private final Lock lock = new ReentrantLock();
    private T firstResult;
    private Throwable firstException;

    @Override
    protected void handleComplete(Subtask<? extends T> subtask) {
      switch (subtask.state()) {
        case FAILED -> {
          lock.lock();
          try {
            if (firstException == null) {
              firstException = subtask.exception();
              shutdown();
            }
          } finally {
            lock.unlock();
          }
        }
        case SUCCESS -> {
          lock.lock();
          try {
            if (firstResult == null) {
              firstResult = subtask.get();
              shutdown();
            }
          } finally {
            lock.unlock();
          }
        }
        case UNAVAILABLE -> super.handleComplete(subtask);
      }
    }

    @Override
    public ShutdownOnResult<T> join() throws InterruptedException {
      super.join();
      return this;
    }

    public T resultOrThrow() throws ExecutionException {
      if (firstException != null) {
        throw new ExecutionException(firstException);
      }
      return firstResult;
    }
  }

  static <T> T race(Callable<T> first, Callable<T> second)
      throws InterruptedException, ExecutionException {
    try (var scope = new ShutdownOnResult<T>()) {
      scope.fork(first);
      scope.fork(second);
      return scope.join().resultOrThrow();
    }
  }

  static <T> T timeout(Duration timeout, Callable<T> task)
      throws InterruptedException, ExecutionException {
    return race(
        task,
        () -> {
          delay(timeout);
          throw new TimeoutException("Timeout of %s reached".formatted(timeout));
        });
  }

  static <T> T timeout2(Duration timeout, Callable<T> task)
      throws InterruptedException, TimeoutException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var result = scope.fork(task);
      scope.joinUntil(Instant.now().plus(timeout));
      return result.get();
    }
  }

  record Bitcoin(String hash) {}

  static Bitcoin mineBitcoin() {
    LOGGER.info("Mining Bitcoin...");
    while (alwaysTrue()) {
      // Empty body
    }
    LOGGER.info("Bitcoin mined!");
    return new Bitcoin("bitcoin-hash");
  }

  static Bitcoin mineBitcoinWithConsciousness() {
    LOGGER.info("Mining Bitcoin...");
    while (alwaysTrue()) {
      if (Thread.currentThread().isInterrupted()) {
        LOGGER.info("Bitcoin mining interrupted");
        return null;
      }
    }
    LOGGER.info("Bitcoin mined!");
    return new Bitcoin("bitcoin-hash");
  }

  private static boolean alwaysTrue() {
    return true;
  }

  public static void main() throws ExecutionException, InterruptedException {

    var repository = new GitHubRepository();
    var service = new FindGitHubUserStructuredConcurrencyService(repository, repository);

    final List<GitHubUser> gitHubUsers =
        service.findGitHubUsers(List.of(new UserId(42L), new UserId(1L)), Duration.ofMillis(700L));
  }
}
