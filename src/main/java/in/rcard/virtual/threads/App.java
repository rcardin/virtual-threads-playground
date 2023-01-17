package in.rcard.virtual.threads;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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
  
  private static Thread createNewVirtualThreadWithFactory() {
    return Thread.ofVirtual().start(() -> System.out.println("Hello from a virtual thread!"));
  }
  
  private static void createNewVirtualThreadWithExecutorService()
      throws ExecutionException, InterruptedException {
    try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
      var future = executorService.submit(() -> System.out.println("Hello from a virtual thread!"));
      future.get();
    }
  }
}
