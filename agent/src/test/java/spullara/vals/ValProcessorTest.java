package spullara.vals;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static spullara.vals.ValProcessor.getCommonSuperClass;

public class ValProcessorTest {
  @Test
  public void testCommonSuperClass() {
    assertEquals(Object.class, getCommonSuperClass(Object.class, Object.class));
    assertEquals(Object.class, getCommonSuperClass(String.class, Object.class));
    assertEquals(Object.class, getCommonSuperClass(String.class, Long.class));
    assertEquals(Number.class, getCommonSuperClass(Long.class, Double.class));
  }
}
