package spullara.vals;

/**
 * Helper methods.
 */
public class Vals {

  private static class ValHolder<T> implements lazyval<T> {

    private T t;

    private ValHolder(T t) {
      this.t = t;
    }

    @Override
    public T get() {
      return t;
    }
  }

  public static <T> val<T> createVal(T t) {
    return new ValHolder<T>(t);
  }

  public static <T> lazyval<T> createLazyVal(T t) {
    return new ValHolder<T>(t);
  }
}
