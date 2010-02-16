/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

/**
 * Encapsulates logic to create a stack trace. This class should only be used in
 * web mode.
 */
public class StackTraceCreator {
  /**
   * This class acts as a deferred-binding hook point to allow more optimal
   * versions to be substituted. This base version simply crawls
   * <code>arguments.callee.caller</code>.
   */
  static class Collector {
    public native JsArrayString collect() /*-{
      var seen = {};
      var toReturn = [];

      // Ignore the collect() and fillInStackTrace call
      var callee = arguments.callee.caller.caller;
      while (callee) {
        var name = this.@com.google.gwt.core.client.impl.StackTraceCreator.Collector::extractName(Ljava/lang/String;)(callee.toString());
        toReturn.push(name);

        // Avoid infinite loop by associating names to function objects.  We
        // record each caller in the withThisName variable to handle functions
        // with identical names but separate identity (such as 'anonymous')
        var keyName = ':' + name;
        var withThisName = seen[keyName];
        if (withThisName) {
          var i, j;
          for (i = 0, j = withThisName.length; i < j; i++) {
            if (withThisName[i] === callee) {
              return toReturn;
            }
          }
        }

        (withThisName || (seen[keyName] = [])).push(callee);
        callee = callee.caller;
      }
      return toReturn;
    }-*/;

    public void createStackTrace(JavaScriptException e) {
      JsArrayString stack = inferFrom(e.getException());

      StackTraceElement[] stackTrace = new StackTraceElement[stack.length()];
      for (int i = 0, j = stackTrace.length; i < j; i++) {
        stackTrace[i] = new StackTraceElement("Unknown", stack.get(i),
            "Unknown source", 0);
      }
      e.setStackTrace(stackTrace);
    }

    public void fillInStackTrace(Throwable t) {
      JsArrayString stack = StackTraceCreator.createStackTrace();
      StackTraceElement[] stackTrace = new StackTraceElement[stack.length()];
      for (int i = 0, j = stackTrace.length; i < j; i++) {
        stackTrace[i] = new StackTraceElement("Unknown", stack.get(i),
            "Unknown source", 0);
      }
      t.setStackTrace(stackTrace);
    }

    /**
     * Attempt to infer the stack from an unknown JavaScriptObject that had been
     * thrown. The default implementation just returns an empty array.
     * 
     * @param e a JavaScriptObject
     */
    public JsArrayString inferFrom(JavaScriptObject e) {
      return JavaScriptObject.createArray().cast();
    }

    /**
     * Extract the name of a function from it's toString() representation.
     * Package-access for testing.
     */
    protected String extractName(String fnToString) {
      return extractNameFromToString(fnToString);
    }

    /**
     * Raise an exception and return it.
     */
    protected native JavaScriptObject makeException() /*-{
      try {
        null.a();
      } catch (e) {
        return e;
      }
    }-*/;
  }

  /**
   * Collaborates with JsStackEmulator.
   */
  static class CollectorEmulated extends Collector {

    @Override
    public JsArrayString collect() {
      JsArrayString toReturn = JsArrayString.createArray().cast();
      JsArray<JavaScriptObject> stack = getStack();
      for (int i = 0, j = getStackDepth(); i < j; i++) {
        String name = stack.get(i) == null ? "anonymous"
            : extractName(stack.get(i).toString());
        // Reverse the order
        toReturn.set(j - i - 1, name);
      }

      return toReturn;
    }

    @Override
    public void createStackTrace(JavaScriptException e) {
      // No-op, relying on initializer call to collect()
    }

    public void fillInStackTrace(Throwable t) {
      JsArrayString stack = collect();
      JsArrayString locations = getLocation();
      StackTraceElement[] stackTrace = new StackTraceElement[stack.length()];
      for (int i = 0, j = stackTrace.length; i < j; i++) {
        // Locations is also backwards
        String location = locations.get(j - i - 1);
        String fileName = "Unknown source";
        int lineNumber = 0;
        if (location != null) {
          int idx = location.indexOf(':');
          if (idx != -1) {
            fileName = location.substring(0, idx);
            lineNumber = Integer.parseInt(location.substring(idx + 1));
          } else {
            lineNumber = Integer.parseInt(location);
          }
        }
        stackTrace[i] = new StackTraceElement("Unknown", stack.get(i),
            fileName, lineNumber);
      }
      t.setStackTrace(stackTrace);
    }

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      throw new RuntimeException("Should not reach here");
    }

    private native JsArrayString getLocation()/*-{
      return $location;
    }-*/;

    private native JsArray<JavaScriptObject> getStack()/*-{
      return $stack;
    }-*/;

    private native int getStackDepth() /*-{
      return $stackDepth;
    }-*/;
  }

  /**
   * Mozilla provides a <code>stack</code> property in thrown objects.
   */
  static class CollectorMoz extends Collector {
    /**
     * This implementation doesn't suffer from the limitations of crawling
     * <code>caller</code> since Mozilla provides proper activation records.
     */
    @Override
    public JsArrayString collect() {
      return splice(inferFrom(makeException()), toSplice());
    }

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      JsArrayString stack = getStack(e);
      for (int i = 0, j = stack.length(); i < j; i++) {
        stack.set(i, extractName(stack.get(i)));
      }
      return stack;
    }

    protected native JsArrayString getStack(JavaScriptObject e) /*-{
      return (e && e.stack) ? e.stack.split('\n') : [];
    }-*/;

    protected int toSplice() {
      return 2;
    }
  }

  /**
   * Chrome uses a slightly different format to Mozilla.
   * 
   * See http://code.google.com/p/v8/source/browse/branches/bleeding_edge/src/
   * messages.js?r=2340#712 for formatting code.
   * 
   * Function calls can be of the four following forms:
   * 
   * <pre>
   * at file.js:1:2
   * at functionName (file.js:1:2)
   * at Type.functionName (file.js:1:2)
   * at Type.functionName [as methodName] (file.js:1:2)
   * </pre>
   */
  static class CollectorChrome extends CollectorMoz {
    @Override
    public JsArrayString collect() {
      JsArrayString res = super.collect();
      if (res.length() == 0) {
        /*
         * Ensure Safari falls back to default Collector implementation.
         * Remember to remove this method call from the stack:
         */
        res = splice(new Collector().collect(), 1);
      }
      return res;
    }

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      JsArrayString stack = super.inferFrom(e);
      if (stack.length() == 0) {
        // Safari should fall back to default Collector:
        return new Collector().inferFrom(e);
      } else {
        // Chrome contains the error itself as the first line of the stack:
        return splice(stack, 1);
      }
    }

    @Override
    protected String extractName(String fnToString) {
      if (fnToString.length() == 0) {
        return "anonymous";
      }

      String toReturn = fnToString.trim();

      // Strip the "at " prefix:
      if (toReturn.startsWith("at ")) {
        toReturn = toReturn.substring(3);
      }

      // Strip bracketed items from the end:
      int index = toReturn.indexOf("[");
      if (index == -1) {
        index = toReturn.indexOf("(");
      }
      if (index == -1) {
        // No bracketed items found, hence no function name available:
        return "anonymous";
      } else {
        // Bracketed items found: strip them off.
        toReturn = toReturn.substring(0, index).trim();
      }

      // Strip the Type off to leave just the functionName:
      index = toReturn.indexOf('.');
      if (index != -1) {
        toReturn = toReturn.substring(index + 1);
      }

      return toReturn.length() > 0 ? toReturn : "anonymous";
    }

    @Override
    protected int toSplice() {
      return 3;
    }
  }

  /**
   * Opera encodes stack trace information in the error's message.
   */
  static class CollectorOpera extends CollectorMoz {
    /**
     * We have much a much simpler format to work with.
     */
    @Override
    protected String extractName(String fnToString) {
      return fnToString.length() == 0 ? "anonymous" : fnToString;
    }

    /**
     * Opera has the function name on every-other line.
     */
    @Override
    protected JsArrayString getStack(JavaScriptObject e) {
      JsArrayString toReturn = getMessage(e);
      assert toReturn.length() % 2 == 0 : "Expecting an even number of lines";

      int i, i2, j;
      for (i = 0, i2 = 0, j = toReturn.length(); i2 < j; i++, i2 += 2) {
        int idx = toReturn.get(i2).lastIndexOf("function ");
        if (idx == -1) {
          toReturn.set(i, "");
        } else {
          toReturn.set(i, toReturn.get(i2).substring(idx + 9).trim());
        }
      }
      setLength(toReturn, i);

      return toReturn;
    }

    @Override
    protected int toSplice() {
      return 3;
    }

    private native JsArrayString getMessage(JavaScriptObject e) /*-{
      return (e && e.message) ? e.message.split('\n') : [];
    }-*/;

    private native void setLength(JsArrayString obj, int length) /*-{
      obj.length = length;
    }-*/;
  }

  /**
   * When compiler.stackMode = strip, we stub out the collector.
   */
  static class CollectorNull extends Collector {
    @Override
    public  JsArrayString collect() {
      return JsArrayString.createArray().cast();
    }

    @Override
    public void createStackTrace(JavaScriptException e) {
      e.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public void fillInStackTrace(Throwable t) {
      t.setStackTrace(new StackTraceElement[0]);
    }
  }
  
  /**
   * Create a stack trace based on a JavaScriptException. This method should
   * only be called in web mode.
   */
  public static void createStackTrace(JavaScriptException e) {
    if (!GWT.isScript()) {
      throw new RuntimeException(
          "StackTraceCreator should only be called in web mode");
    }

    GWT.<Collector> create(Collector.class).createStackTrace(e);
  }

  /**
   * Fill in a stack trace based on the current execution stack. This method
   * should only be called in web mode.
   */
  public static void fillInStackTrace(Throwable t) {
    if (!GWT.isScript()) {
      throw new RuntimeException(
          "StackTraceCreator should only be called in web mode");
    }

    GWT.<Collector> create(Collector.class).fillInStackTrace(t);
  }

  /**
   * Create a stack trace based on the current execution stack. This method
   * should only be called in web mode.
   */
  static JsArrayString createStackTrace() {
    if (!GWT.isScript()) {
      throw new RuntimeException(
          "StackTraceCreator should only be called in web mode");
    }

    return GWT.<Collector> create(Collector.class).collect();
  }

  static String extractNameFromToString(String fnToString) {
    String toReturn = "";
    fnToString = fnToString.trim();
    int index = fnToString.indexOf("(");
    if (index != -1) {
      int start = fnToString.startsWith("function") ? 8 : 0;
      toReturn = fnToString.substring(start, index).trim();
    }

    return toReturn.length() > 0 ? toReturn : "anonymous";
  }

  private static native JsArrayString splice(JsArrayString arr, int length) /*-{
    (arr.length >= length) && arr.splice(0, length);
    return arr;
  }-*/;
}
