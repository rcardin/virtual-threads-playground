package in.rcard.virtual.threads;

import java.time.Duration;

public class App {
  public static void main(String[] args) {
    stackOverFlowErrorExample();
  }

  private static void stackOverFlowErrorExample() {
    for (int i = 0; i < 100_000; i++) {
      new Thread(() -> {
        try {
          Thread.sleep(Duration.ofSeconds(1L));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }).start();
    }
  }
}
