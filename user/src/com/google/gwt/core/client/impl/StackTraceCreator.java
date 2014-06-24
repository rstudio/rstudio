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

    public abstract void collect(Object t, Object jsThrown);

    public abstract StackTraceElement[] getStackTrace(Object t);
  }

  /**
   * This legacy {@link Collector} simply crawls <code>arguments.callee.caller</code> for browsers
   * that doesn't support {@code Error.stack} property.
   */
  static class CollectorLegacy extends Collector {

    @Override
    public native void collect(Object t, Object thrownIgnored) /*-{
      var seen = {};
      t.fnStack = [];

      // Ignore the collect() call
      var callee = arguments.callee.caller;
      while (callee) {
        var name = @StackTraceCreator::getFunctionName(*)(callee);
        t.fnStack.push(name);

        // Avoid infinite loop by associating names to function objects.  We
        // record each caller in the withThisName variable to handle functions
        // with identical names but separate identity (such as 'anonymous')
        var keyName = ':' + name;
        var withThisName = seen[keyName];
        if (withThisName) {
          var i, j;
          for (i = 0, j = withThisName.length; i < j; i++) {
            if (withThisName[i] === callee) {
              return;
            }
          }
        }

        (withThisName || (seen[keyName] = [])).push(callee);
        callee = callee.caller;
      }
    }-*/;

    @Override
    public StackTraceElement[] getStackTrace(Object t) {
      JsArrayString stack = getFnStack(t);

      int length = stack.length();
      StackTraceElement[] stackTrace = new StackTraceElement[length];
      for (int i = 0; i < length; i++) {
        stackTrace[i] = new StackTraceElement(UNKNOWN, stack.get(i), null, LINE_NUMBER_UNKNOWN);
      }
      return stackTrace;
    }
  }

  /**
   * Collaborates with JsStackEmulator.
   */
  static final class CollectorEmulated extends Collector {

    @Override
    public native void collect(Object t, Object jsThrownIgnored) /*-{
      t.fnStack = [];
      for (var i = 0; i < $stackDepth; i++) {
        var location = $location[i];
        var fn = $stack[i];
        var name = fn ? @StackTraceCreator::getFunctionName(*)(fn) : @StackTraceCreator::ANONYMOUS;

        // Reverse the order
        t.fnStack[$stackDepth - i - 1] = [name, location];
      }
    }-*/;

    @Override
    public StackTraceElement[] getStackTrace(Object t) {
      JsArray<JsArrayString> stack = getFnStack(t).cast();

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
   * Modern browsers provide a <code>stack</code> property in thrown objects.
   */
  static class CollectorModern extends Collector {

    static {
      increaseStackTraceLimit();
    }

    // As of today, only available in IE10+ and Chrome.
    private static native void increaseStackTraceLimit() /*-{
      // TODO(cromwellian) make this a configurable?
      Error.stackTraceLimit = 64;
    }-*/;

    @Override
    public native void collect(Object t, Object jsThrown) /*-{
      // TODO(goktug): optimize for Chrome by not evaluating stack (use Error.captureStackTrace)
      t.stack = (jsThrown && jsThrown.stack) || @CollectorModern::makeException()().stack;
     }-*/;

    private static native JavaScriptObject makeException() /*-{
      // TODO(goktug): new Error().stack is broken for htmlunit:
      // https://sourceforge.net/p/htmlunit/bugs/1606/
      try {
        null.a();
      } catch (e) {
        return e;
      }
    }-*/;

    private JsArrayString inferFrom(Object t) {
      JsArrayString stack = split(t);
      for (int i = 0, j = stack.length(); i < j; i++) {
        stack.set(i, extractName(stack.get(i)));
      }

      if (stack.length() > 0 && stack.get(0).startsWith(ANONYMOUS + "@@")) {
        // Chrome, IE10+ contains the error msg as the first line of stack (iOS, Firefox doesn't).
        stack = splice(stack, 1);
      }
      return stack;
    }

    private String extractName(String fnToString) {
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
        // No bracketed items found, try '@' (used by iOS & Firefox).
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

      // IE has also special naming for anonymous functions.
      if (toReturn.equals("Anonymous function")) {
         toReturn = "";
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
    public StackTraceElement[] getStackTrace(Object t) {
      JsArrayString stack = inferFrom(t);

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

          if (lastColon != -1 && endFileUrl != -1) {
              fileName = location.substring(0, endFileUrl);
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
  static class CollectorModernNoSourceMap extends CollectorModern {
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
    public void collect(Object ignored, Object jsThrownIgnored) {
      // Nothing to do
    }

    @Override
    public StackTraceElement[] getStackTrace(Object ignored) {
      return new StackTraceElement[0];
    }
  }

  /**
   * Collect necessary information to construct stack trace trace later in time.
   */
  public static void captureStackTrace(Throwable throwable, Object reference) {
    collector.collect(throwable, reference);
  }

  public static StackTraceElement[] constructJavaStackTrace(Throwable thrown) {
    StackTraceElement[] stackTrace = collector.getStackTrace(thrown);
    return dropInternalFrames(stackTrace);
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
    // Ensure old Safari falls back to legacy Collector implementation.
    collector = (c instanceof CollectorModern && !supportsErrorStack()) ? new CollectorLegacy() : c;
  }

  private static native boolean supportsErrorStack() /*-{
    return "stack" in new Error; // Checked via 'in' to avoid execution of stack getter in Chrome
  }-*/;

  private static native JsArrayString getFnStack(Object e) /*-{
    return (e && e.fnStack && e.fnStack instanceof Array) ? e.fnStack : [];
  }-*/;

  private static native String getFunctionName(JavaScriptObject fn) /*-{
    return fn.name || (fn.name = @StackTraceCreator::extractFunctionName(*)(fn.toString()));
  }-*/;

  // Visible for testing
  static native String extractFunctionName(String fnName) /*-{
    var fnRE = /function(?:\s+([\w$]+))?\s*\(/;
    var match = fnRE.exec(fnName);
    return (match && match[1]) || @StackTraceCreator::ANONYMOUS;
  }-*/;

  private static native JsArrayString split(Object e) /*-{
    return e.stack ? e.stack.split('\n') : [];
  }-*/;

  private static native <T> T splice(T arr, int length) /*-{
    (arr.length >= length) && arr.splice(0, length);
    return arr;
  }-*/;
}
