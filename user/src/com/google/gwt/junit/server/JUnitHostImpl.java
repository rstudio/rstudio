/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.junit.server;

import com.google.gwt.junit.JUnitMessageQueue;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.client.impl.ExceptionWrapper;
import com.google.gwt.junit.client.impl.JUnitHost;
import com.google.gwt.junit.client.impl.StackTraceWrapper;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * An RPC servlet that serves as a proxy to GWTUnitTestShell. Enables
 * communication between the unit test code running in a browser and the real
 * test process.
 */
public class JUnitHostImpl extends RemoteServiceServlet implements JUnitHost {

  /**
   * A hook into GWTUnitTestShell, the underlying unit test process.
   */
  private static JUnitMessageQueue sHost = null;

  /**
   * A maximum timeout to wait for the test system to respond with the next
   * test. Practically speaking, the test system should respond nearly instantly
   * if there are furthur tests to run.
   */
  private static final int TIME_TO_WAIT_FOR_TESTNAME = 5000;

  /**
   * Tries to grab the GWTUnitTestShell sHost environment to communicate with the
   * real test process.
   */
  private static synchronized JUnitMessageQueue getHost() {
    if (sHost == null) {
      sHost = JUnitShell.getMessageQueue();
      if (sHost == null) {
        throw new InvocationException(
          "Unable to find JUnitShell; is this servlet running under GWTTestCase?");
      }
    }
    return sHost;
  }

  /**
   * Simple helper method to set inaccessible fields via reflection.
   */
  private static void setField(Class cls, String fieldName, Object obj,
      Object value) throws SecurityException, NoSuchFieldException,
      IllegalArgumentException, IllegalAccessException {
    Field fld = cls.getDeclaredField(fieldName);
    fld.setAccessible(true);
    fld.set(obj, value);
  }

  public String getFirstMethod(String testClassName) {
    return getHost().getNextTestName(testClassName, TIME_TO_WAIT_FOR_TESTNAME);
  }

  public String reportResultsAndGetNextMethod(String testClassName,
      ExceptionWrapper ew) {
    JUnitMessageQueue host = getHost();
    host.reportResults(testClassName, deserialize(ew));
    return host.getNextTestName(testClassName, TIME_TO_WAIT_FOR_TESTNAME);
  }

  /**
   * Deserializes an ExceptionWrapper back into a Throwable.
   */
  private Throwable deserialize(ExceptionWrapper ew) {
    if (ew == null) {
      return null;
    }

    Throwable ex = null;
    Throwable cause = deserialize(ew.cause);
    try {
      Class exClass = Class.forName(ew.typeName);
      try {
        // try ExType(String, Throwable)
        Constructor ctor = exClass.getDeclaredConstructor(new Class[]{
          String.class, Throwable.class});
        ctor.setAccessible(true);
        ex = (Throwable) ctor.newInstance(new Object[]{ew.message, cause});
      } catch (Throwable e) {
        // try ExType(String)
        try {
          Constructor ctor = exClass
            .getDeclaredConstructor(new Class[]{String.class});
          ctor.setAccessible(true);
          ex = (Throwable) ctor.newInstance(new Object[]{ew.message});
          ex.initCause(cause);
        } catch (Throwable e2) {
          // try ExType(Throwable)
          try {
            Constructor ctor = exClass
              .getDeclaredConstructor(new Class[]{Throwable.class});
            ctor.setAccessible(true);
            ex = (Throwable) ctor.newInstance(new Object[]{cause});
            setField(exClass, "detailMessage", ex, ew.message);
          } catch (Throwable e3) {
            // try ExType()
            try {
              Constructor ctor = exClass.getDeclaredConstructor(null);
              ctor.setAccessible(true);
              ex = (Throwable) ctor.newInstance(null);
              ex.initCause(cause);
              setField(exClass, "detailMessage", ex, ew.message);
            } catch (Throwable e4) {
              // we're out of options
              this.log("Failed to deserialize exception of type '"
                + ew.typeName + "'; no available constructor", e4);

              // fall through
            }
          }
        }
      }

    } catch (Throwable e) {
      this.log("Failed to deserialize exception of type '" + ew.typeName + "'",
        e);
    }

    if (ex == null) {
      ex = new RuntimeException(ew.typeName + ": " + ew.message, cause);
    }

    ex.setStackTrace(deserialize(ew.stackTrace));
    return ex;
  }

  /**
   * Deserializes a StackTraceWrapper back into a StackTraceElement.
   */
  private StackTraceElement deserialize(StackTraceWrapper stw) {
    StackTraceElement ste = null;
    Object[] args = new Object[]{
      stw.className, stw.methodName, stw.fileName, new Integer(stw.lineNumber)};
    try {
      try {
        // Try the 4-arg ctor (JRE 1.5)
        Constructor ctor = StackTraceElement.class
          .getDeclaredConstructor(new Class[]{
            String.class, String.class, String.class, int.class});
        ctor.setAccessible(true);
        ste = (StackTraceElement) ctor.newInstance(args);
      } catch (NoSuchMethodException e) {
        // Okay, see if there's a zero-arg ctor we can use instead (JRE 1.4.2)
        Constructor ctor = StackTraceElement.class.getDeclaredConstructor(null);
        ctor.setAccessible(true);
        ste = (StackTraceElement) ctor.newInstance(null);
        setField(StackTraceElement.class, "declaringClass", ste, args[0]);
        setField(StackTraceElement.class, "methodName", ste, args[1]);
        setField(StackTraceElement.class, "fileName", ste, args[2]);
        setField(StackTraceElement.class, "lineNumber", ste, args[3]);
      }
    } catch (Throwable e) {
      this.log("Error creating stack trace", e);
    }
    return ste;
  }

  /**
   * Deserializes a StackTraceWrapper[] back into a StackTraceElement[].
   */
  private StackTraceElement[] deserialize(StackTraceWrapper[] stackTrace) {
    int len = stackTrace.length;
    StackTraceElement[] result = new StackTraceElement[len];
    for (int i = 0; i < len; ++i) {
      result[i] = deserialize(stackTrace[i]);
    }
    return result;
  }

}
