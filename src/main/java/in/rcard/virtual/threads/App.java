package in.rcard.virtual.threads;

public class App {

  // FIXME Uncomment when Lombok will support Java 23
  //  static final Logger logger = LoggerFactory.getLogger(App.class);
  //
  //  public static void main(String[] args) {
  //    virtualThreadLocal();
  //  }
  //
  //  private static void stackOverFlowErrorExample() {
  //    for (int i = 0; i < 100_000; i++) {
  //      new Thread(() -> sleep(Duration.ofSeconds(1L))).start();
  //    }
  //  }
  //
  //  @SneakyThrows
  //  static void concurrentMorningRoutine() {
  //    var bathTime = bathTime();
  //    var boilingWater = boilingWater();
  //    bathTime.join();
  //    boilingWater.join();
  //  }
  //
  //  @SneakyThrows
  //  static void concurrentMorningRoutineUsingExecutors() {
  //    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
  //      var bathTime =
  //          executor.submit(
  //              () -> {
  //                log("I'm going to take a bath");
  //                sleep(Duration.ofMillis(500L));
  //                log("I'm done with the bath");
  //              });
  //      var boilingWater =
  //          executor.submit(
  //              () -> {
  //                log("I'm going to boil some water");
  //                sleep(Duration.ofSeconds(1L));
  //                log("I'm done with the water");
  //              });
  //      bathTime.get();
  //      boilingWater.get();
  //    }
  //  }
  //
  //  @SneakyThrows
  //  static void concurrentMorningRoutineUsingExecutorsWithName() {
  //    final ThreadFactory factory = Thread.ofVirtual().name("routine-", 0).factory();
  //    try (var executor = Executors.newThreadPerTaskExecutor(factory)) {
  //      var bathTime =
  //          executor.submit(
  //              () -> {
  //                log("I'm going to take a bath");
  //                sleep(Duration.ofMillis(500L));
  //                log("I'm done with the bath");
  //              });
  //      var boilingWater =
  //          executor.submit(
  //              () -> {
  //                log("I'm going to boil some water");
  //                sleep(Duration.ofSeconds(1L));
  //                log("I'm done with the water");
  //              });
  //      bathTime.get();
  //      boilingWater.get();
  //    }
  //  }
  //
  //  static Thread bathTime() {
  //    return virtualThread(
  //        "bath-time",
  //        () -> {
  //          log("I'm going to take a bath");
  //          sleep(Duration.ofMillis(500L));
  //          log("I'm done with the bath");
  //        });
  //  }
  //
  //  static Thread boilingWater() {
  //    return virtualThread(
  //        "boilWater",
  //        () -> {
  //          log("I'm going to boil some water");
  //          sleep(Duration.ofSeconds(1L));
  //          log("I'm done with the water");
  //        });
  //  }
  //
  //  @SneakyThrows
  //  static void workingHardRoutine() {
  //    var workingHard = workingHard();
  //    var takeABreak = takeABreak();
  //    workingHard.join();
  //    takeABreak.join();
  //  }
  //
  //  @SneakyThrows
  //  static void workingConsciousnessRoutine() {
  //    var workingConsciousness = workingConsciousness();
  //    var takeABreak = takeABreak();
  //    workingConsciousness.join();
  //    takeABreak.join();
  //  }
  //
  //  static Thread workingHard() {
  //    return virtualThread(
  //        "Working hard",
  //        () -> {
  //          log("I'm working hard");
  //          while (alwaysTrue()) {
  //            // Do nothing
  //          }
  //          sleep(Duration.ofMillis(100L));
  //          log("I'm done with working hard");
  //        });
  //  }
  //
  //  static Thread workingConsciousness() {
  //    return virtualThread(
  //        "Working consciousness",
  //        () -> {
  //          log("I'm working hard");
  //          while (alwaysTrue()) {
  //            sleep(Duration.ofMillis(100L));
  //          }
  //          log("I'm done with working hard");
  //        });
  //  }
  //
  //  static boolean alwaysTrue() {
  //    return true;
  //  }
  //
  //  static Thread takeABreak() {
  //    return virtualThread(
  //        "Take a break",
  //        () -> {
  //          log("I'm going to take a break");
  //          sleep(Duration.ofSeconds(1L));
  //          log("I'm done with the break");
  //        });
  //  }
  //
  //  @SneakyThrows
  //  static void twoEmployeesInTheOffice() {
  //    var riccardo = goToTheToilet();
  //    var daniel = takeABreak();
  //    riccardo.join();
  //    daniel.join();
  //  }
  //
  //  @SneakyThrows
  //  static void twoEmployeesInTheOfficeWithLock() {
  //    var riccardo = goToTheToiletWithLock();
  //    var daniel = takeABreak();
  //    riccardo.join();
  //    daniel.join();
  //  }
  //
  //  static Bathroom bathroom = new Bathroom();
  //
  //  static Thread goToTheToilet() {
  //    return virtualThread("Go to the toilet", () -> bathroom.useTheToilet());
  //  }
  //
  //  static Thread goToTheToiletWithLock() {
  //    return virtualThread("Go to the toilet", () -> bathroom.useTheToiletWithLock());
  //  }
  //
  //  static class Bathroom {
  //
  //    private final Lock lock = new ReentrantLock();
  //
  //    synchronized void useTheToilet() {
  //      log("I'm going to use the toilet");
  //      sleep(Duration.ofSeconds(1L));
  //      log("I'm done with the toilet");
  //    }
  //
  //    @SneakyThrows
  //    void useTheToiletWithLock() {
  //      if (lock.tryLock(10, TimeUnit.SECONDS)) {
  //        try {
  //          log("I'm going to use the toilet");
  //          sleep(Duration.ofSeconds(1L));
  //          log("I'm done with the toilet");
  //        } finally {
  //          lock.unlock();
  //        }
  //      }
  //    }
  //  }
  //
  //  static void viewCarrierThreadPoolSize() {
  //    final ThreadFactory factory = Thread.ofVirtual().name("routine-", 0).factory();
  //    try (var executor = Executors.newThreadPerTaskExecutor(factory)) {
  //      IntStream.range(0, numberOfCores() + 1)
  //          .forEach(
  //              i ->
  //                  executor.submit(
  //                      () -> {
  //                        log("Hello, I'm a virtual thread number " + i);
  //                        sleep(Duration.ofSeconds(1L));
  //                      }));
  //    }
  //  }
  //
  //  static int numberOfCores() {
  //    return Runtime.getRuntime().availableProcessors();
  //  }
  //
  //  @SneakyThrows
  //  static void sleep(Duration duration) {
  //    Thread.sleep(duration);
  //  }
  //
  //  static Thread virtualThread(String name, Runnable runnable) {
  //    return Thread.ofVirtual().name(name).start(runnable);
  //  }
  //
  //  static void log(String message) {
  //    logger.info("{} | " + message, Thread.currentThread());
  //  }
  //
  //  static ThreadLocal<String> context = new ThreadLocal<>();
  //
  //  @SneakyThrows
  //  static void platformThreadLocal() {
  //    var thread1 = Thread.ofPlatform().name("thread-1").start(() -> {
  //      context.set("thread-1");
  //      sleep(Duration.ofSeconds(1L));
  //      log("Hey, my name is " + context.get());
  //    });
  //    var thread2 = Thread.ofPlatform().name("thread-2").start(() -> {
  //      context.set("thread-2");
  //      sleep(Duration.ofSeconds(1L));
  //      log("Hey, my name is " + context.get());
  //    });
  //    thread1.join();
  //    thread2.join();
  //  }
  //
  //  @SneakyThrows
  //  static void virtualThreadLocal() {
  //    var virtualThread1 = Thread.ofVirtual().name("thread-1").start(() -> {
  //      context.set("thread-1");
  //      sleep(Duration.ofSeconds(1L));
  //      log("Hey, my name is " + context.get());
  //    });
  //    var virtualThread2 = Thread.ofVirtual().name("thread-2").start(() -> {
  //      context.set("thread-2");
  //      sleep(Duration.ofSeconds(1L));
  //      log("Hey, my name is " + context.get());
  //    });
  //    virtualThread1.join();
  //    virtualThread2.join();
  //  }
}
