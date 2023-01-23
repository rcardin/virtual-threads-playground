package in.rcard.virtual.threads;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    concurrentMorningRoutineUsingExecutorsWithName();
  }

  private static void stackOverFlowErrorExample() {
    for (int i = 0; i < 100_000; i++) {
      new Thread(() -> sleep(Duration.ofSeconds(1L))).start();
    }
  }

  @SneakyThrows
  static void concurrentMorningRoutine() {
    var vt1 = bathTime();
    var vt2 = boilingWater();
    vt1.join();
    vt2.join();
  }

  @SneakyThrows
  static void concurrentMorningRoutineUsingExecutors() {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var f1 =
          executor.submit(
              () -> {
                logger.info("I'm going to take a bath");
                sleep(Duration.ofMillis(500L));
                logger.info("I'm done with the bath");
              });
      var f2 =
          executor.submit(
              () -> {
                logger.info("I'm going to boil some water");
                sleep(Duration.ofSeconds(1L));
                logger.info("I'm done with the water");
              });
      f1.get();
      f2.get();
    }
  }

  @SneakyThrows
  static void concurrentMorningRoutineUsingExecutorsWithName() {
    final ThreadFactory factory = Thread.ofVirtual().name("routine-", 0).factory();
    try (var executor =
        Executors.newThreadPerTaskExecutor(factory)) {
      var f1 =
          executor.submit(
              () -> {
                logger.info("I'm going to take a bath");
                sleep(Duration.ofMillis(500L));
                logger.info("I'm done with the bath");
              });
      var f2 =
          executor.submit(
              () -> {
                logger.info("I'm going to boil some water");
                sleep(Duration.ofSeconds(1L));
                logger.info("I'm done with the water");
              });
      f1.get();
      f2.get();
    }
  }

  static Thread bathTime() {
    return virtualThread(
        "Bath time",
        () -> {
          logger.info("I'm going to take a bath");
          sleep(Duration.ofMillis(500L));
          logger.info("I'm done with the bath");
        });
  }

  static Thread boilingWater() {
    return virtualThread(
        "Boil some water",
        () -> {
          logger.info("I'm going to boil some water");
          sleep(Duration.ofSeconds(1L));
          logger.info("I'm done with the water");
        });
  }

  @SneakyThrows
  private static void sleep(Duration duration) {
    Thread.sleep(duration);
  }

  private static Thread virtualThread(String name, Runnable runnable) {
    return Thread.ofVirtual().name(name).start(runnable);
  }
}
