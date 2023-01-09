package in.rcard.virtual.threads;

import jdk.incubator.concurrent.StructuredTaskScope;

public class App {
  public static void main(String[] args) throws Exception {
    try (var scope = new StructuredTaskScope<Void>()) {
      scope.fork(() -> delayPrint(1000, "Hello,"));
      scope.fork(() -> delayPrint(2000, "World!"));
      scope.join();
    }
    System.out.println("Done!");
  }

  private static Void delayPrint(long delay, String message) throws Exception {
    Thread.sleep(delay);
    System.out.println(message);
    return null;
  }
}
