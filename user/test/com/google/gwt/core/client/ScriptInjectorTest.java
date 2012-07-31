/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.core.client;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.ScriptInjector.FromString;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link ScriptInjector}
 */
public class ScriptInjectorTest extends GWTTestCase {
  private static boolean browserChecked = false;
  private static final int CHECK_DELAY = 100;

  private static boolean isIE = false;
  private static final int TEST_DELAY = 10000;

  /**
   * Check if the browser is IE6,7,8,9.
   * 
   * @return <code>true</code> if the browser is IE6, IE7, IE8, IE9
   *         <code>false</code> any other browser
   */
  static boolean isIE() {
    if (!browserChecked) {
      isIE = isIEImpl();
      browserChecked = true;
    }
    return isIE;
  }

  private static native boolean isIEImpl() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("msie") != -1) {
      return true;
    }
    return false;
  }-*/;

  private native boolean isSafari3OrBefore() /*-{
    return @com.google.gwt.dom.client.DOMImplWebkit::isWebkit525OrBefore()();
  }-*/;
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /**
   * Install a script in the same window as GWT.
   */
  public void testInjectDirectThisWindow() {
    delayTestFinish(TEST_DELAY);
    String scriptBody = "__ti1_var__ = 1;";
    assertFalse(nativeTest1Worked());
    new FromString(scriptBody).inject();
    boolean worked = nativeTest1Worked();
    JavaScriptObject scriptElement = findScriptTextInThisWindow(scriptBody);
    if (!isIE()) {
      cleanupThisWindow("__ti1_var__", scriptElement);
      assertFalse("cleanup failed", nativeTest1Worked());
    }
    assertTrue("__ti1_var not set in this window", worked);
    assertNull("script element 1 not removed by injection", scriptElement);
    finishTest();
  }

  /**
   * Install a script in the top window.
   */
  public void testInjectDirectTopWindow() {
    String scriptBody = "__ti2_var__ = 2;";
    assertFalse(nativeTest2Worked());
    ScriptInjector.fromString(scriptBody).setWindow(ScriptInjector.TOP_WINDOW).inject();
    boolean worked = nativeTest2Worked();
    JavaScriptObject scriptElement = findScriptTextInTopWindow(scriptBody);
    if (!isIE()) {
      cleanupTopWindow("__ti2_var__", scriptElement);
      assertTrue("__ti2_var not set in top window", worked);
    }
    assertNull("script element 2 not removed by injection", scriptElement);
  }

  /**
   * Install a script in the same window as GWT, turn off the tag removal.
   */
  public void testInjectDirectWithoutRemoveTag() {
    assertFalse(nativeTest3Worked());
    String scriptBody = "__ti3_var__ = 3;";
    new FromString(scriptBody).setRemoveTag(false).inject();
    boolean worked = nativeTest3Worked();
    JavaScriptObject scriptElement = findScriptTextInThisWindow(scriptBody);
    if (!isIE()) {
      cleanupThisWindow("__ti3_var__", scriptElement);
      assertFalse("cleanup failed", nativeTest3Worked());
    }
    assertTrue(worked);
    assertNotNull("script element 3 should have been left in DOM", scriptElement);
  }

  /**
   * Inject an absolute URL on this window.
   */
  public void testInjectUrlAbsolute() {
    delayTestFinish(TEST_DELAY);
    final String scriptUrl = GWT.getModuleBaseForStaticFiles() + "script_injector_test_absolute.js";
    assertFalse(nativeInjectUrlAbsoluteWorked());
    ScriptInjector.fromUrl(scriptUrl).setCallback(new Callback<Void, Exception>() {

      @Override
      public void onFailure(Exception reason) {
        assertNotNull(reason);
        fail("Injection failed: " + reason.toString());
      }

      @Override
      public void onSuccess(Void result) {
        assertTrue(nativeInjectUrlAbsoluteWorked());
        finishTest();
      }

    }).inject();
  }

  /**
   * Inject an absolute URL on the top level window.
   */
  public void testInjectUrlAbsoluteTop() {
    delayTestFinish(TEST_DELAY);
    final String scriptUrl = GWT.getModuleBaseForStaticFiles() + "script_injector_test_absolute_top.js";
    assertFalse(nativeAbsoluteTopUrlIsLoaded());
    ScriptInjector.fromUrl(scriptUrl).setWindow(ScriptInjector.TOP_WINDOW).setCallback(
        new Callback<Void, Exception>() {

          @Override
          public void onFailure(Exception reason) {
            assertNotNull(reason);
            fail("Injection failed: " + reason.toString());
          }

          @Override
          public void onSuccess(Void result) {
            assertTrue(nativeAbsoluteTopUrlIsLoaded());
            finishTest();
          }
        }).inject();
  }

  /**
   * This script injection should fail and fire the onFailure callback.
   * 
   * Note, the onerror mechanism used to trigger the failure event is a modern browser
   * feature.
   * 
   * On IE, the script.onerror tag has been documented, but busted for <a
   * href=
   * "http://stackoverflow.com/questions/2027849/how-to-trigger-script-onerror-in-internet-explorer/2032014#2032014"
   * >aeons</a>.
   * 
   */
  public void testInjectUrlFail() {
    if (isIE() || isSafari3OrBefore()) {
      return;
    }
    
    delayTestFinish(TEST_DELAY);
    final String scriptUrl = "uNkNoWn_sCrIpT_404.js";
    JavaScriptObject injectedElement =
        ScriptInjector.fromUrl(scriptUrl).setCallback(new Callback<Void, Exception>() {

          @Override
          public void onFailure(Exception reason) {
            assertNotNull(reason);
            finishTest();
          }

          @Override
          public void onSuccess(Void result) {
            fail("Injection unexpectedly succeeded.");
          }
        }).inject();
    assertNotNull(injectedElement);
  }

  /**
   * Install a script in the same window as GWT by URL
   */
  public void testInjectUrlThisWindow() {
    this.delayTestFinish(TEST_DELAY);
    final String scriptUrl = "script_injector_test4.js";
    assertFalse(nativeTest4Worked());
    final JavaScriptObject injectedElement = ScriptInjector.fromUrl(scriptUrl).inject();

    // We'll check using a callback in another test. This test will poll to see
    // that the script had an effect.
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      int numLoops = 0;

      @Override
      public boolean execute() {
        numLoops++;
        boolean worked = nativeTest4Worked();
        if (!worked && (numLoops * CHECK_DELAY < TEST_DELAY)) {
          return true;
        }
        JavaScriptObject scriptElement = findScriptUrlInThisWindow(scriptUrl);
        if (!isIE()) {
          cleanupThisWindow("__ti4_var__", scriptElement);
          assertFalse("cleanup failed", nativeTest4Worked());
        }
        assertTrue("__ti4_var not set in this window", worked);
        assertNotNull("script element 4 not found", scriptElement);
        assertEquals(injectedElement, scriptElement);
        finishTest();

        // never reached
        return false;
      }
    }, CHECK_DELAY);
    assertNotNull(injectedElement);
  }

  /**
   * Install a script in the same window as GWT by URL
   */
  public void testInjectUrlThisWindowCallback() {
    delayTestFinish(TEST_DELAY);
    final String scriptUrl = "script_injector_test5.js";
    assertFalse(nativeTest5Worked());
    JavaScriptObject injectedElement =
        ScriptInjector.fromUrl(scriptUrl).setCallback(new Callback<Void, Exception>() {
          @Override
          public void onFailure(Exception reason) {
            assertNotNull(reason);
            fail("Injection failed: " + reason.toString());
          }

          @Override
          public void onSuccess(Void result) {
            boolean worked = nativeTest5Worked();
            JavaScriptObject scriptElement = findScriptUrlInThisWindow(scriptUrl);
            if (!isIE()) {
              cleanupThisWindow("__ti5_var__", scriptElement);
              assertFalse("cleanup failed", nativeTest5Worked());
            }
            assertTrue("__ti5_var not set in this window", worked);
            assertNotNull("script element 5 not found", scriptElement);
            finishTest();
          }
        }).inject();
    assertNotNull(injectedElement);
  }

  /**
   * Install a script in the top window by URL
   */
  public void testInjectUrlTopWindow() {
    final String scriptUrl = "script_injector_test6.js";
    assertFalse(nativeTest6Worked());
    JavaScriptObject injectedElement =
        ScriptInjector.fromUrl(scriptUrl).setWindow(ScriptInjector.TOP_WINDOW).inject();
    // We'll check using a callback in another test. This test will poll to see
    // that the script had an effect.
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      int numLoops = 0;

      @Override
      public boolean execute() {
        numLoops++;

        boolean worked = nativeTest6Worked();
        if (!worked && (numLoops * CHECK_DELAY < TEST_DELAY)) {
          return true;
        }
        JavaScriptObject scriptElement = findScriptUrlInTopWindow(scriptUrl);
        if (!isIE()) {
          cleanupTopWindow("__ti6_var__", scriptElement);
          assertFalse("cleanup failed", nativeTest6Worked());
        }
        assertTrue("__ti6_var not set in top window", worked);
        assertNotNull("script element 6 not found", scriptElement);
        finishTest();
        // never reached
        return false;
      }
    }, CHECK_DELAY);
    assertNotNull(injectedElement);
  }

  /**
   * Install a script in the top window by URL
   */
  public void testInjectUrlTopWindowCallback() {
    delayTestFinish(TEST_DELAY);
    final String scriptUrl = "script_injector_test7.js";
    assertFalse(nativeTest7Worked());
    JavaScriptObject injectedElement =
        ScriptInjector.fromUrl(scriptUrl).setWindow(ScriptInjector.TOP_WINDOW).setCallback(
            new Callback<Void, Exception>() {

              @Override
              public void onFailure(Exception reason) {
                assertNotNull(reason);
                fail("Injection failed: " + reason.toString());
              }

              @Override
              public void onSuccess(Void result) {
                boolean worked = nativeTest7Worked();
                JavaScriptObject scriptElement = findScriptUrlInTopWindow(scriptUrl);
                if (!isIE()) {
                  cleanupTopWindow("__ti7_var__", scriptElement);
                  assertFalse("cleanup failed", nativeTest7Worked());
                }
                assertTrue("__ti7_var not set in top window", worked);
                assertNotNull("script element 7 not found", scriptElement);
                finishTest();
              }
            }).inject();
    assertNotNull(injectedElement);
  }

  private void cleanupThisWindow(String property, JavaScriptObject scriptElement) {
    cleanupWindow(nativeThisWindow(), property, scriptElement);
  }

  private void cleanupTopWindow(String property, JavaScriptObject scriptElement) {
    cleanupWindow(nativeTopWindow(), property, scriptElement);
  }

  private native void cleanupWindow(JavaScriptObject wnd, String property,
      JavaScriptObject scriptElement) /*-{
    delete wnd[property];
    if (scriptElement) {
      scriptElement.parentNode.removeChild(scriptElement);
    }
  }-*/;

  private JavaScriptObject findScriptTextInThisWindow(String text) {
    return nativeFindScriptText(nativeThisWindow(), text);
  }

  private JavaScriptObject findScriptTextInTopWindow(String text) {
    return nativeFindScriptText(nativeTopWindow(), text);
  }

  private JavaScriptObject findScriptUrlInThisWindow(String url) {
    return nativeFindScriptUrl(nativeThisWindow(), url);
  }

  private JavaScriptObject findScriptUrlInTopWindow(String url) {
    return nativeFindScriptUrl(nativeTopWindow(), url);
  }

  private native boolean nativeAbsoluteTopUrlIsLoaded() /*-{
    return !!$wnd["__tiabsolutetop_var__"] && $wnd["__tiabsolutetop_var__"] == 102;
  }-*/;

  private native JavaScriptObject nativeFindScriptText(JavaScriptObject wnd, String text) /*-{
    var scripts = wnd.document.getElementsByTagName("script");
    for ( var i = 0; i < scripts.length; ++i) {
      if (scripts[i].text.match("^" + text)) {
        return scripts[i];
      }
    }
    return null;
  }-*/;

  /**
   * Won't work for all urls, uses a regular expression match
   */
  private native JavaScriptObject nativeFindScriptUrl(JavaScriptObject wnd, String url) /*-{
    var scripts = wnd.document.getElementsByTagName("script");
    for ( var i = 0; i < scripts.length; ++i) {
      if (scripts[i].src.match(url)) {
        return scripts[i];
      }
    }
    return null;
  }-*/;

  private native boolean nativeInjectUrlAbsoluteWorked() /*-{
    return !!window["__tiabsolute_var__"] && window["__tiabsolute_var__"] == 101;
  }-*/;

  private native boolean nativeTest1Worked() /*-{
    return !!window["__ti1_var__"] && window["__ti1_var__"] == 1;
  }-*/;

  private native boolean nativeTest2Worked() /*-{
    return !!$wnd["__ti2_var__"] && $wnd["__ti2_var__"] == 2;
  }-*/;

  private native boolean nativeTest3Worked() /*-{
    return !!window["__ti3_var__"] && window["__ti3_var__"] == 3;
  }-*/;

  private native boolean nativeTest4Worked() /*-{
    return !!window["__ti4_var__"] && window["__ti4_var__"] == 4;
  }-*/;

  private native boolean nativeTest5Worked() /*-{
    return !!window["__ti5_var__"] && window["__ti5_var__"] == 5;
  }-*/;

  private native boolean nativeTest6Worked() /*-{
    return !!$wnd["__ti6_var__"] && $wnd["__ti6_var__"] == 6;
  }-*/;

  private native boolean nativeTest7Worked() /*-{
    return !!$wnd["__ti7_var__"] && $wnd["__ti7_var__"] == 7;
  }-*/;

  private native JavaScriptObject nativeThisWindow() /*-{
    return window;
  }-*/;

  private native JavaScriptObject nativeTopWindow() /*-{
    return $wnd;
  }-*/;
}
