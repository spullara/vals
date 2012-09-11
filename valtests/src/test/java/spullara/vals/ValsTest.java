package spullara.vals;

import org.junit.Test;

import static spullara.vals.Vals.createVal;

/**
 * Test making vals and lazy vals.
 */
public class ValsTest {
  public static val<String> name() {
    return createVal("name" + System.currentTimeMillis());
  }

  @Test
  public void testAgent() throws InterruptedException, NoSuchFieldException {
    String first = name().get();
    System.out.println(first);
    Thread.sleep(10);
    String second = name().get();
    System.out.println(second);
    if (!first.equals(second)) {
      System.out.println("Failed");
    }

    System.out.println(ValsTest.class.getDeclaredField("__val_name"));
  }
}
