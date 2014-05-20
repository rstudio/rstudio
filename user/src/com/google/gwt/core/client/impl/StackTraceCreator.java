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
 * Production Mode.
 */
public class StackTraceCreator {

  /**
   * Maximum # of frames to look for {@link Throwable#fillInStackTrace()} in the generated stack
   * trace. This is just a safe guard just in case if {@code fillInStackTrace} doesn't show up in
   * the stack trace for some reason.
   */
  private static final int DROP_FRAME_LIMIT = 5;

  /**
   * Line number used in a stack trace when it is unknown.
   */
  private static final int LINE_NUMBER_UNKNOWN = -1;

  /**
   * Replacement for function names that cannot be extracted from a stack.
   */
  private static final String ANONYMOUS = "anonymous";

  /**
   * Replacement for class or file names that cannot be extracted from a stack.
   */
  private static final String UNKNOWN = "Unknown";

  /**
   * This class acts as a deferred-binding hook point to allow more optimal versions to be
   * substituted.
   */
  abstract static class Collector {

    public abstract JavaScriptObject collect();

    protected StackTraceElement[] getStackTrace(JsArrayString stack) {
      if (stack.length() == 0) {
        return null;
      }
      int length = stack.length();
      StackTraceElement[] stackTrace = new StackTraceElement[length];
      for (int i = 0; i < length; i++) {
        stackTrace[i] = new StackTraceElement(UNKNOWN, stack.get(i), null, LINE_NUMBER_UNKNOWN);
      }
      return stackTrace;
    }

    /**
     * Attempt to infer the stack from an unknown JavaScriptObject that had been thrown.
     */
    public abstract JsArrayString inferFrom(JavaScriptObject e);
  }

  /**
   * This legacy {@link Collector} simply crawls <code>arguments.callee.caller</code> for browsers
   * that doesn't support {@code Error.stack} property.
   */
  static class CollectorLegacy extends Collector {

    @Override
    public native JavaScriptObject collect() /*-{
      var seen = {};
      var toReturn = [];

      // Ignore the collect() call
      var callee = arguments.callee.caller;
      while (callee) {
        var name = @StackTraceCreator::getFunctionName(*)(callee);
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
              return { fnStack: toReturn };
            }
          }
        }

        (withThisName || (seen[keyName] = [])).push(callee);
        callee = callee.caller;
      }
      return { fnStack: toReturn };
    }-*/;

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      return getFnStack(e);
    }
  }

  /**
   * Collaborates with JsStackEmulator.
   */
  static final class CollectorEmulated extends Collector {

    @Override
    public native JavaScriptObject collect() /*-{
      var toReturn = [];
      for (var i = 0; i < $stackDepth; i++) {
        var location = $location[i];
        var fn = $stack[i];
        var name = fn ? @StackTraceCreator::getFunctionName(*)(fn) : @StackTraceCreator::ANONYMOUS;

        // Reverse the order
        toReturn[$stackDepth - i - 1] = [name, location];
      }
      return { fnStack: toReturn };
    }-*/;

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      return getFnStack(e);
    }

    @Override
    protected StackTraceElement[] getStackTrace(JsArrayString st) {
      JsArray<JsArrayString> stack = st.cast();

      if (stack.length() == 0) {
        return null;
      }
      StackTraceElement[] stackTrace = new StackTraceElement[stack.length()];
      for (int i = 0; i < stackTrace.length; i++) {
        JsArrayString frame = stack.get(i);
        String name = frame.get(0);
        String location = frame.get(1);

        String fileName = null;
        int lineNumber = LINE_NUMBER_UNKNOWN;
        if (location != null) {
          int idx = location.indexOf(':');
          if (idx != -1) {
            fileName = location.substring(0, idx);
            lineNumber = parseInt(location.substring(idx + 1));
          } else {
            lineNumber = parseInt(location);
          }
        }
        stackTrace[i] = new StackTraceElement(UNKNOWN, name, fileName, lineNumber);
      }
      return stackTrace;
    }
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
    public JavaScriptObject collect() {
      return makeException();
    }

    /**
     * Raise an exception and return it.
     */
    private static native JavaScriptObject makeException() /*-{
      try {
        null.a();
      } catch (e) {
        return e;
      }
    }-*/;

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      JsArrayString stack = getStack(e);
      for (int i = 0, j = stack.length(); i < j; i++) {
        stack.set(i, extractName(stack.get(i)));
      }
      return stack;
    }

    private native JsArrayString getStack(JavaScriptObject e) /*-{
      return (e && e.stack) ? e.stack.split('\n') : [];
    }-*/;

    /**
     * Extract the name of a function from it's toString() representation.
     */
    protected String extractName(String fnToString) {
      String toReturn = "";
      fnToString = fnToString.trim();
      int index = fnToString.indexOf("(");
      int start = fnToString.startsWith("function") ? 8 : 0;
      if (index == -1) {
        // Firefox 14 does not include parenthesis and uses '@' symbol instead to terminate symbol
        index = fnToString.indexOf('@');
        /**
         * Firefox 14 doesn't return strings like 'function()' for anonymous methods, so
         * we assert a space must trail 'function' keyword for a method named 'functionName', e.g.
         * functionName:file.js:2 won't accidentally strip off the 'function' prefix which is part
         * of the name.
         */
        start = fnToString.startsWith("function ") ? 9 : 0;
      }
      if (index != -1) {
        toReturn = fnToString.substring(start, index).trim();
      }
      return toReturn.length() > 0 ? toReturn : ANONYMOUS;
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

    static {
      increaseChromeStackTraceLimit();
    }

    // TODO(cromwellian) make this a configurable?
    private static native void increaseChromeStackTraceLimit() /*-{
      // 128 seems like a reasonable maximum
      Error.stackTraceLimit = 128;
    }-*/;

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      JsArrayString stack = super.inferFrom(e);
      if (stack.length() > 0 && stack.get(0).startsWith(ANONYMOUS + "@@")) {
        // Chrome contains the error itself as the first line of the stack (iOS doesn't).
        stack = splice(stack, 1);
      }
      return stack;
    }

    @Override
    protected String extractName(String fnToString) {
      String extractedName = ANONYMOUS;
      String location = "";

      if (fnToString.length() == 0) {
        return extractedName;
      }

      String toReturn = fnToString.trim();

      // Strip the "at " prefix:
      if (toReturn.startsWith("at ")) {
        toReturn = toReturn.substring(3);
      }

      toReturn = stripSquareBrackets(toReturn);

      int index = toReturn.indexOf("(");
      if (index == -1) {
        // No bracketed items found, try '@' (used by iOS).
        index = toReturn.indexOf("@");
        if (index == -1) {
          // No bracketed items nor '@' found, hence no function name available
          location = toReturn;
          toReturn = "";
        } else {
          location = toReturn.substring(index + 1).trim();
          toReturn = toReturn.substring(0, index).trim();
        }
      } else {
        // Bracketed items found: strip them off, parse location info
        int closeParen = toReturn.indexOf(")", index);
        location = toReturn.substring(index + 1, closeParen);
        toReturn = toReturn.substring(0, index).trim();
      }

      // Strip the Type off t
      index = toReturn.indexOf('.');
      if (index != -1) {
        toReturn = toReturn.substring(index + 1);
      }
      return (toReturn.length() > 0 ? toReturn : ANONYMOUS) + "@@" + location;
    }

    private native String stripSquareBrackets(String toReturn) /*-{
      return toReturn.replace(/\[.*?\]/g,"")
    }-*/;

    protected int replaceIfNoSourceMap(int line) {
         return line;
    }

    @Override
    protected StackTraceElement[] getStackTrace(JsArrayString stack) {
      int length = stack.length();
      StackTraceElement[] stackTrace = new StackTraceElement[length];
      for (int i = 0; i < length; i++) {
        String stackElements[] = stack.get(i).split("@@");

        int line = LINE_NUMBER_UNKNOWN;
        int col = -1;
        String fileName = UNKNOWN;
        if (stackElements.length == 2 && stackElements[1] != null) {
          String location = stackElements[1];
          // colon between line and column
          int lastColon = location.lastIndexOf(':');
          // colon between file url and line number
          int endFileUrl = location.lastIndexOf(':', lastColon - 1);
          fileName = location.substring(0, endFileUrl);

          if (lastColon != -1 && endFileUrl != -1) {
              line = parseInt(location.substring(endFileUrl + 1, lastColon));
              col = parseInt(location.substring(lastColon + 1));
          }
        }
        stackTrace[i] = new StackTraceElement(UNKNOWN, stackElements[0], fileName + "@" + col,
            replaceIfNoSourceMap(line < 0 ? LINE_NUMBER_UNKNOWN : line));
      }
      return stackTrace;
    }
  }

  /**
   * Subclass that forces reported line numbers to -1 (fetch from symbolMap) if source maps are
   * disabled.
   */
  static class CollectorChromeNoSourceMap extends CollectorChrome {
    @Override
    protected int replaceIfNoSourceMap(int line) {
      return LINE_NUMBER_UNKNOWN;
    }
  }

  private static native int parseInt(String number) /*-{
    return parseInt(number) || @StackTraceCreator::LINE_NUMBER_UNKNOWN;
  }-*/;

  /**
   * When compiler.stackMode = strip, we stub out the collector.
   */
  static class CollectorNull extends Collector {
    @Override
    public  JsArrayString collect() {
      return JsArrayString.createArray().cast();
    }

    @Override
    protected StackTraceElement[] getStackTrace(JsArrayString stack) {
      return null;
    }

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      return null;
    }
  }

  /**
   * Create a stack trace based on a JavaScriptException. This method should
   * only be called in Production Mode.
   */
  public static void createStackTrace(JavaScriptException e) {
    constructStackTrace(e, e.getThrown(), false);
  }

  /**
   * Fill in a stack trace based on the current execution stack. This method
   * should only be called in Production Mode.
   */
  public static void fillInStackTrace(Throwable t) {
    constructStackTrace(t, collector.collect(), true);
  }

  private static void constructStackTrace(Throwable t, Object thrown, boolean strip) {
    JavaScriptObject e = (thrown instanceof JavaScriptObject) ? (JavaScriptObject) thrown : null;
    JsArrayString stack = collector.inferFrom(e);
    StackTraceElement[] stackTrace = collector.getStackTrace(stack);
    if (stackTrace != null) {
      if (strip) {
        stackTrace = dropInternalFrames(stackTrace);
      }
      t.setStackTrace(stackTrace);
    }
  }

  private static StackTraceElement[] dropInternalFrames(StackTraceElement[] stackTrace) {
    final String dropFrameUntilFnName = Impl.getNameOf("@java.lang.Throwable::fillInStackTrace()");

    int numberOfFrameToSearch = Math.min(stackTrace.length, DROP_FRAME_LIMIT);
    for (int i = 0; i < numberOfFrameToSearch; i++) {
      if (stackTrace[i].getMethodName().equals(dropFrameUntilFnName)) {
        return splice(stackTrace, i + 1);
      }
    }

    return stackTrace;
  }

  // Visible for testing
  static final Collector collector;

  static {
    Collector c = GWT.create(Collector.class);
    // Ensure old Safari falls back to default Collector implementation.
    collector = (c instanceof CollectorChrome && !supportsErrorStack()) ? new CollectorLegacy() : c;
  }

  private static native boolean supportsErrorStack() /*-{
    return "stack" in new Error; // Checked via 'in' to avoid execution of stack getter in Chrome
  }-*/;

  private static native JsArrayString getFnStack(JavaScriptObject e) /*-{
    return (e && e.fnStack && e.fnStack instanceof Array) ? e.fnStack : [];
  }-*/;

  private static native String getFunctionName(JavaScriptObject fn) /*-{
    return fn.name || (fn.name = @StackTraceCreator::extractFunctionName(*)(fn.toString()));
  }-*/;

  static native String extractFunctionName(String fnName) /*-{
    var fnRE = /function(?:\s+([\w$]+))?\s*\(/;
    return (fnRE.test(fnName) && RegExp.$1) || @StackTraceCreator::ANONYMOUS;
  }-*/;

  private static native <T> T splice(T arr, int length) /*-{
    (arr.length >= length) && arr.splice(0, length);
    return arr;
  }-*/;
}
