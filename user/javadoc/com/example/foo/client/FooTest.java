package com.example.foo.client;

import com.google.gwt.junit.client.GWTTestCase;

public class FooTest extends GWTTestCase {

  /*
   * Specifies a module to use when running this test case. The returned
   * module must cause the source for this class to be included.
   * 
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "com.example.foo.Foo";
  }

  public void testStuff() {
    assertTrue(2 + 2 == 4);
  }
}
