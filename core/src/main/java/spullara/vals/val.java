package spullara.vals;

/**
 * Marking interface. At runtime methods and declarations are transformed to return T instead.
 * The get calls are also removed.
 */
public interface val<T> {
  T get();
}
