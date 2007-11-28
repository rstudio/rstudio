package com.google.gwt.dev.util;

import junit.framework.TestCase;

import java.net.URL;

/** Pure junit test of Utility functionality*/
public class UtilityTest extends TestCase{
  
  
  
  /** Tests that URLAsChars correctly processes unicode*/
  public void testUnicode() {
    URL r = this.getClass().getResource("unicodeTest.txt");
    assertNotNull(r);
   
    char[] x = Util.readURLAsChars(r);
    assertEquals(2,x.length);
    char a = '\u4F60';
    assertEquals(x[0],a);
    assertEquals(x[1],'\u597D');
  }
  
  
}
