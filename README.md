When running with the Java agent, the name() method always returns the 
same thing and is set at the time ValsTest is initialized.

```java
package spullara.vals;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static spullara.vals.Vals.createVal;

/**
 * Test making vals and lazy vals.
 */
public class ValsTest {
  public static val<String> name() {
    return createVal("name" + System.currentTimeMillis());
  }

  @Test
  public void test() throws InterruptedException {
    String first = name().get();
    System.out.println(first);
    Thread.sleep(10);
    String second = name().get();
    System.out.println(second);
    assertEquals(first, second);
  }
}
```