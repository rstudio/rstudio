package com.google.gwt.module.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests waiting on a single external script.
 * @see com.google.gwt.module.client.DoubleScriptInjectionTest
 */
public class SingleScriptInjectionTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.module.SingleScriptInjectionTest";
  }

  /**
   * Coordinates with external ScriptInjectionTest1 JavaScript file in the
   * public folder, which uses a timer to delay its readiness indicator. This
   * proves that the test truly won't run until the script-ready function
   * defined in the module is satisfied.
   */
  public void testWaitForScript() {
    String answer = isScriptOneReady();
    assertEquals("yes1", answer);
  }

  /**
   * The native method called here is defined in SingleScriptInjectionTest1.
   */
  public static native String isScriptOneReady() /*-{
   return $wnd.isScriptOneReady();
   }-*/;
}
