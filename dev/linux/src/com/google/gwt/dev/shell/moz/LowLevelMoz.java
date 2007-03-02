/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Various low-level helper methods for dealing with Gecko.
 */
public class LowLevelMoz {

  /**
   * Provides interface for methods to be exposed on JavaScript side.
   */
  public interface DispatchMethod {
    /**
     * Invoke a Java method from JavaScript.
     * 
     * @param jsthis the wrapped Java object to invoke
     * @param jsargs an array of JavaScript values to pass as parameters
     * @param returnValue the JavaScript value in which to store the returned
     *     value
     */
    void invoke(int jscontext, int jsthis, int[] jsargs, int returnValue);
  }

  /**
   * Provides interface for objects to be exposed to JavaScript code.
   */
  public interface DispatchObject {
    /**
     * Retrieve a field from an object.
     * 
     * @param name the name of the field
     * @param value pointer to the JsRootedValue to receive the field value
     */
    void getField(String name, int value);

    Object getTarget();

    /**
     * Set the value of a field on an object.
     * 
     * @param name the name of the field
     * @param value pointer to the JsRootedValue to store into the field
     */
    void setField(String name, int value);
  }

  interface ExternalFactory {
    ExternalObject createExternalObject();

    boolean matchesDOMWindow(int domWindow);
  }

  /**
   * TODO: rip this whole thing out if possible and use DispatchObject like on
   * Safari.
   */
  interface ExternalObject {
    boolean gwtOnLoad(int scriptGlobalObject, String moduleName);
  }

  private static int invokeCount = 0;
  private static Vector sExternalFactories = new Vector();
  private static boolean sInitialized = false;

  /**
   * Executes JavaScript code, retaining file and line information.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param code The JavaScript code to execute
   * @param file A file name associated with the code
   * @param line A line number associated with the code.
   */
  public static void executeScriptWithInfo(int scriptObject, String code,
      String file, int line) {
    if (!_executeScriptWithInfo(scriptObject, code, file, line)) {
      throw new RuntimeException(file + "(" + line
          + "): Failed to execute script: " + code);
    }
  }

  public static String getMozillaDirectory() {
    String installPath = Utility.getInstallPath();
    try {
      // try to make absolute
      installPath = new File(installPath).getCanonicalPath();
    } catch (IOException e) {
      // ignore problems, failures will occur when the libs try to load
    }
    return installPath + "/mozilla-1.7.12";
  }

  public static synchronized void init() {
    // Force LowLevel initialization to load gwt-ll
    LowLevel.init();
    if (!sInitialized) {
      if (!_registerExternalFactoryHandler()) {
        throw new RuntimeException(
            "Failed to register external factory handler.");
      }
      sInitialized = true;
    }
  }

  /**
   * Invokes a method implemented in JavaScript.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param methodName the method name on jsthis to call
   * @param jsthis A wrapped java object as a JsRootedValue pointer
   * @param jsargs the arguments to pass to the method as JsRootedValue pointers
   * 
   * @throws RuntimeException if the invoke fails
   */
  public static void invoke(int scriptObject, String methodName,
      int jsthis, int[] jsargs, int retval) {
    if (!_invoke(scriptObject, methodName, jsthis, jsargs, retval)) {
      throw new RuntimeException("Failed to invoke native method: "
          + methodName + " with " + jsargs.length + " arguments.");
    }
  }

  /**
   * Call this to raise an exception in JavaScript before returning control.
   * Currently, the JavaScript exception throw is always null.
   * 
   * @param jscontext A JSContext pointer as a Java int
   */
  public static void raiseJavaScriptException(int jscontext) {
    if (!_raiseJavaScriptException(jscontext)) {
      throw new RuntimeException(
          "Failed to raise Java Exception into JavaScript.");
    }
  }

  /**
   * BrowserWindows register here so that if their contained window gets a call
   * to window.external, the call can be routed correctly by nsIDOMWindow
   * pointer.
   * 
   * @param externalFactory the factory to register
   */
  public static void registerExternalFactory(ExternalFactory externalFactory) {
    synchronized (sExternalFactories) {
      sExternalFactories.add(externalFactory);
    }
  }

  /**
   * Unregisters an existing registration.
   * 
   * @param externalFactory the factory to unregister
   */
  public static void unregisterExternalFactory(ExternalFactory externalFactory) {
    synchronized (sExternalFactories) {
      sExternalFactories.remove(externalFactory);
    }
  }

  /**
   * Called from native code to create an external object for a particular
   * window.
   * 
   * @param domWindow an nsIDOMWindow to check against our ExternalFactories map
   * @return a new ExternalObject
   */
  protected static ExternalObject createExternalObjectForDOMWindow(int domWindow) {
    for (Iterator iter = sExternalFactories.iterator(); iter.hasNext();) {
      ExternalFactory fac = (ExternalFactory) iter.next();
      if (fac.matchesDOMWindow(domWindow)) {
        return fac.createExternalObject();
      }
    }
    return null;
  }

  /**
   * Called from native code to do tracing.
   * 
   * @param s the string to trace
   */
  protected static void trace(String s) {
    System.out.println(s);
    System.out.flush();
  }

  // CHECKSTYLE_NAMING_OFF: Non JSNI native code may have leading '_'s.
  private static native boolean _executeScript(int scriptObject, String code);

  private static native boolean _executeScriptWithInfo(int scriptObject,
      String newScript, String file, int line);

  /**
   * Native method for invoking a JavaScript method.
   * 
   * @param scriptObject nsIScriptGlobalObject* as an int
   * @param methodName name of JavaScript method
   * @param jsThisInt JavaScript object to invoke the method on, as a
   *   JsRootedValue int
   * @param jsArgsInt array of arguments, as an array of JsRootedValue ints
   * @param jsRetValint pointer to JsRootedValue to receive return value
   * @return true on success
   */
  private static native boolean _invoke(int scriptObject, String methodName,
      int jsThisInt, int[] jsArgsInt, int jsRetValInt);

  private static native boolean _raiseJavaScriptException(int jscontext);

  private static native boolean _registerExternalFactoryHandler();

  // CHECKSTYLE_NAMING_ON

  /**
   * Print debug information for a JS method invocation.
   * TODO(jat): remove this method
   * 
   * @param methodName the name of the JS method being invoked
   * @param jsthis the JS object with the named method
   * @param jsargs an array of arguments to the method
   */
  private static void printInvocationParams(String methodName, JsValueMoz jsthis, JsValueMoz[] jsargs) {
    System.out.println("LowLevelMoz.invoke:");
    System.out.println(" method = " + methodName);
    System.out.println(" # args = " + (jsargs.length));
    System.out.println(" jsthis = " + jsthis.toString());
    for (int i = 0; i < jsargs.length; ++i) {
      System.out.println(" jsarg[" + i + "] = " + jsargs[i].toString());
    }
    System.out.println("");
  }

  /**
   * Not instantiable.
   */
  private LowLevelMoz() {
  }
}
