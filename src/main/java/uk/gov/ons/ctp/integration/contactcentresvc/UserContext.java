package uk.gov.ons.ctp.integration.contactcentresvc;

public final class UserContext {
  private static ThreadLocal<String> ctx = new ThreadLocal<>();

  public static void set(String userId) {
    ctx.set(userId);
  }

  public static void clear() {
    ctx.set(null);
  }

  public static String get() {
    return ctx.get();
  }
}
