package in.rcard.virtual.threads;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) throws Exception {
    createNewVirtualThreadWithFactory();
  }

  private static void stackOverFlowErrorExample() {
    for (int i = 0; i < 100_000; i++) {
      new Thread(
              () -> {
                try {
                  Thread.sleep(Duration.ofSeconds(1L));
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              })
          .start();
    }
  }

  private static void createNewVirtualThreadWithFactory() throws InterruptedException {
    final Thread virtualThread = Thread.ofVirtual()
                             .start(() -> logger.info("Hello from a virtual thread!"));
    virtualThread.setName("pippo");
    virtualThread.join(1000L);
  }

  private static void createNewVirtualThreadWithExecutorService()
      throws ExecutionException, InterruptedException {
    try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
      var future = executorService.submit(() -> logger.info("Hello from a virtual thread!"));
      future.get();
    }
  }
}
