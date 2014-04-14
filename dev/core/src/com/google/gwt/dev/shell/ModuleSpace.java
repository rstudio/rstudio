/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * The interface to the low-level browser, this class serves as a 'domain' for a
 * module, loading all of its classes in a separate, isolated class loader. This
 * allows us to run multiple modules, both in succession and simultaneously.
 */
public abstract class ModuleSpace implements ShellJavaScriptHost {

  private static ThreadLocal<Throwable> sCaughtJavaExceptionObject = new ThreadLocal<Throwable>();

  private static ThreadLocal<Throwable> sThrownJavaExceptionObject = new ThreadLocal<Throwable>();

  /**
   * Logger is thread local.
   */
  private static ThreadLocal<TreeLogger> threadLocalLogger = new ThreadLocal<TreeLogger>();

  public static void setThrownJavaException(Throwable t) {
    sThrownJavaExceptionObject.set(t);
  }

  /**
   * Equivalent to {@link #createJavaScriptException(ClassLoader,Object,String)
   * createJavaScriptException(cl, exception, "")}.
   */
  protected static RuntimeException createJavaScriptException(ClassLoader cl,
      Object exception) {
    return createJavaScriptException(cl, exception, "");
  }

  /**
   * Create a JavaScriptException object. This must be done reflectively, since
   * this class will have been loaded from a ClassLoader other than the
   * session's thread.
   */
  protected static RuntimeException createJavaScriptException(ClassLoader cl,
      Object exception, String message) {
    Exception caught;
    try {
      Class<?> javaScriptExceptionClass = Class.forName(
          "com.google.gwt.core.client.JavaScriptException", true, cl);
      Constructor<?> ctor = javaScriptExceptionClass.getDeclaredConstructor(
          Object.class, String.class);
      return (RuntimeException) ctor.newInstance(new Object[] {exception, message});
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (SecurityException e) {
      caught = e;
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e;
    }
    throw new RuntimeException("Error creating JavaScriptException", caught);
  }

  protected static TreeLogger getLogger() {
    return threadLocalLogger.get();
  }

  /**
   * Get the original thrown object. If the exception is JavaScriptException,
   * gets the object wrapped by a JavaScriptException. We have to do
   * this reflectively, since the JavaScriptException object is from an
   * arbitrary classloader. If the object is not a JavaScriptException, or is
   * not from the given ClassLoader, or the exception is not set we'll return
   * exception itself.
   */
  static Object getThrownObject(ClassLoader cl, Object exception) {
    if (exception.getClass().getClassLoader() != cl) {
      return exception;
    }

    Exception caught;
    try {
      Class<?> javaScriptExceptionClass = Class.forName(
          "com.google.gwt.core.client.JavaScriptException", true, cl);

      if (!javaScriptExceptionClass.isInstance(exception)) {
        // Not a JavaScriptException
        return exception;
      }
      Method isThrownSet = javaScriptExceptionClass.getMethod("isThrownSet");
      if (!((Boolean) isThrownSet.invoke(exception))) {
        return exception;
      }
      Method getThrown = javaScriptExceptionClass.getMethod("getThrown");
      return getThrown.invoke(exception);
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e;
    }
    throw new RuntimeException("Error getting exception value", caught);
  }

  protected final ModuleSpaceHost host;

  private final TreeLogger logger;

  private final String moduleName;

  protected ModuleSpace(TreeLogger logger, ModuleSpaceHost host,
      String moduleName) {
    this.host = host;
    this.moduleName = moduleName;
    this.logger = logger;
    threadLocalLogger.set(host.getLogger());
  }

  public void dispose() {
    // Clear our class loader.
    getIsolatedClassLoader().clear();
  }

  @Override
  public void exceptionCaught(Object exception) {
    Throwable caught;
    Throwable thrown = sThrownJavaExceptionObject.get();
    if (thrown != null && isExceptionSame(thrown, exception)) {
      // The caught exception was thrown by us.
      caught = thrown;
      sThrownJavaExceptionObject.set(null);
    } else if (exception instanceof Throwable) {
      caught = (Throwable) exception;
    } else {
      caught = createJavaScriptException(getIsolatedClassLoader(), exception);
      // Remove excess stack frames from the new exception.
      caught.fillInStackTrace();
      StackTraceElement[] trace = caught.getStackTrace();
      assert trace.length > 1;
      assert trace[1].getClassName().equals(JavaScriptHost.class.getName());
      assert trace[1].getMethodName().equals("exceptionCaught");
      StackTraceElement[] newTrace = new StackTraceElement[trace.length - 1];
      System.arraycopy(trace, 1, newTrace, 0, newTrace.length);
      caught.setStackTrace(newTrace);
    }
    sCaughtJavaExceptionObject.set(caught);
  }

  /**
   * Get the module name.
   *
   * @return the module name
   */
  public String getModuleName() {
    return moduleName;
  }

  @Override
  public boolean invokeNativeBoolean(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a boolean");
    Boolean value = JsValueGlue.get(result, getIsolatedClassLoader(),
        boolean.class, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a boolean");
    }
    return value.booleanValue();
  }

  @Override
  public byte invokeNativeByte(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a byte");
    Byte value = JsValueGlue.get(result, null, Byte.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a byte");
    }
    return value.byteValue();
  }

  @Override
  public char invokeNativeChar(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a char");
    Character value = JsValueGlue.get(result, null, Character.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a char");
    }
    return value.charValue();
  }

  @Override
  public double invokeNativeDouble(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a double");
    Double value = JsValueGlue.get(result, null, Double.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a double");
    }
    return value.doubleValue();
  }

  @Override
  public float invokeNativeFloat(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a float");
    Float value = JsValueGlue.get(result, null, Float.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a float");
    }
    return value.floatValue();
  }

  @Override
  public int invokeNativeInt(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "an int");
    Integer value = JsValueGlue.get(result, null, Integer.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected an int");
    }
    return value.intValue();
  }

  @Override
  public long invokeNativeLong(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a long");
    Long value = JsValueGlue.get(result, null, Long.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a long");
    }
    return value.longValue();
  }

  @Override
  public Object invokeNativeObject(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a Java object");
    return JsValueGlue.get(result, getIsolatedClassLoader(), Object.class,
        msgPrefix);
  }

  @Override
  public short invokeNativeShort(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = composeResultErrorMsgPrefix(name, "a short");
    Short value = JsValueGlue.get(result, null, Short.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a short");
    }
    return value.shortValue();
  }

  @Override
  public void invokeNativeVoid(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    if (!result.isUndefined()) {
      logger.log(
          TreeLogger.WARN,
          "JSNI method '"
              + name
              + "' returned a value of type "
              + result.getTypeString()
              + " but was declared void; it should not have returned a value at all",
          null);
    }
  }

  /**
   * Allows client-side code to log to the tree logger.
   */
  @Override
  public void log(String message, Throwable e) {
    TreeLogger.Type type = TreeLogger.INFO;
    if (e != null) {
      type = TreeLogger.ERROR;
    }
    // Log at the top level for visibility.
    TreeLogger t = getLogger();
    if (t != null) {
      getLogger().log(type, message, e);
    }
  }

  /**
   * Runs the module's user startup code.
   */
  public final void onLoad(TreeLogger logger) throws UnableToCompleteException {
    Event moduleSpaceLoadEvent = SpeedTracerLogger.start(DevModeEventType.MODULE_SPACE_LOAD);

    // Tell the host we're ready for business.
    //
    host.onModuleReady(this);

    // Make sure we can resolve JSNI references to static Java names.
    //
    try {
      createStaticDispatcher(logger);
      Object staticDispatch = getStaticDispatcher();
      invokeNativeVoid("__defineStatic", null, new Class[] {Object.class},
          new Object[] {staticDispatch});
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Unable to initialize static dispatcher", e);
      throw new UnableToCompleteException();
    }

    // Actually run user code.
    //
    String entryPointTypeName = null;
    try {
      // Set up GWT-entry code
      Class<?> implClass = loadClassFromSourceName("com.google.gwt.core.client.impl.Impl");
      Method registerEntry = implClass.getDeclaredMethod("registerEntry");
      registerEntry.setAccessible(true);
      registerEntry.invoke(null);

      Method enter = implClass.getDeclaredMethod("enter");
      enter.setAccessible(true);
      enter.invoke(null);

      String[] entryPoints = host.getEntryPointTypeNames();
      if (entryPoints.length > 0) {
        try {
          for (int i = 0; i < entryPoints.length; i++) {
            entryPointTypeName = entryPoints[i];
            Method onModuleLoad = null;
            Object module;

            // Try to initialize EntryPoint, else throw up glass panel
            try {
              Class<?> clazz = loadClassFromSourceName(entryPointTypeName);
              try {
                onModuleLoad = clazz.getMethod("onModuleLoad");
                if (!Modifier.isStatic(onModuleLoad.getModifiers())) {
                  // it's non-static, so we need to rebind the class
                  onModuleLoad = null;
                }
              } catch (NoSuchMethodException e) {
                // okay, try rebinding it; maybe the rebind result will have one
              }
              module = null;
              if (onModuleLoad == null) {
                module = rebindAndCreate(entryPointTypeName);
                onModuleLoad = module.getClass().getMethod("onModuleLoad");
                // Record the rebound name of the class for stats (below).
                entryPointTypeName = module.getClass().getName().replace(
                    '$', '.');
              }
            } catch (Throwable e) {
              displayErrorGlassPanel(
                  "EntryPoint initialization exception", entryPointTypeName, e);
              throw e;
            }

            // Try to invoke onModuleLoad, else throw up glass panel
            try {
              onModuleLoad.setAccessible(true);
              invokeNativeVoid("fireOnModuleLoadStart", null,
                  new Class[]{String.class}, new Object[]{entryPointTypeName});

              Event onModuleLoadEvent = SpeedTracerLogger.start(
                  DevModeEventType.ON_MODULE_LOAD);
              try {
                onModuleLoad.invoke(module);
              } finally {
                onModuleLoadEvent.end();
              }
            } catch (Throwable e) {
              displayErrorGlassPanel(
                  "onModuleLoad() threw an exception", entryPointTypeName, e);
              throw e;
            }
          }
        } finally {
          Method exit = implClass.getDeclaredMethod("exit", boolean.class);
          exit.setAccessible(true);
          exit.invoke(null, true);
        }
      } else {
        logger.log(
            TreeLogger.WARN,
            "The module has no entry points defined, so onModuleLoad() will never be called",
            null);
      }
    } catch (Throwable e) {
      Throwable caught = e;

      if (e instanceof InvocationTargetException) {
        caught = ((InvocationTargetException) e).getTargetException();
      }

      if (caught instanceof ExceptionInInitializerError) {
        caught = ((ExceptionInInitializerError) caught).getException();
      }

      String unableToLoadMessage = "Unable to load module entry point class "
          + entryPointTypeName;
      if (caught != null) {
        unableToLoadMessage += " (see associated exception for details)";
      }
      logger.log(TreeLogger.ERROR, unableToLoadMessage, caught);
      throw new UnableToCompleteException();
    } finally {
      moduleSpaceLoadEvent.end();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T rebindAndCreate(String requestedClassName)
      throws UnableToCompleteException {
    assert Name.isBinaryName(requestedClassName);
    Throwable caught = null;
    String msg = null;
    String resultName = null;
    Class<?> resolvedClass = null;

    Event moduleSpaceRebindAndCreate =
        SpeedTracerLogger.start(DevModeEventType.MODULE_SPACE_REBIND_AND_CREATE);
    try {
      // Rebind operates on source-level names.
      //
      String sourceName = BinaryName.toSourceName(requestedClassName);
      resultName = rebind(sourceName);
      moduleSpaceRebindAndCreate.addData(
          "Requested Class", requestedClassName, "Result Name", resultName);
      resolvedClass = loadClassFromSourceName(resultName);
      if (Modifier.isAbstract(resolvedClass.getModifiers())) {
        msg = "Deferred binding result type '" + resultName
            + "' should not be abstract";
      } else {
        Constructor<?> ctor = resolvedClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return (T) ctor.newInstance();
      }
    } catch (ClassNotFoundException e) {
      msg = "Could not load deferred binding result type '" + resultName + "'";
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (ExceptionInInitializerError e) {
      caught = e.getException();
    } catch (NoSuchMethodException e) {
      // If it is a nested class and not declared as static,
      // then it's not accessible from outside.
      //
      if (resolvedClass.getEnclosingClass() != null
          && !Modifier.isStatic(resolvedClass.getModifiers())) {
        msg = "Rebind result '" + resultName
        + " is a non-static inner class";
      } else {
        msg = "Rebind result '" + resultName
        + "' has no default (zero argument) constructors.";
      }
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    } finally {
      moduleSpaceRebindAndCreate.end();
    }

    // Always log here because sometimes this method gets called from static
    // initializers and other unusual places, which can obscure the problem.
    //
    if (msg == null) {
      msg = "Failed to create an instance of '" + requestedClassName
          + "' via deferred binding ";
    }
    host.getLogger().log(TreeLogger.ERROR, msg, caught);
    throw new UnableToCompleteException();
  }

  protected String createNativeMethodInjector(String jsniSignature,
      String[] paramNames, String js) {
    String newScript = "window[\"" + jsniSignature + "\"] = function(";

    for (int i = 0; i < paramNames.length; ++i) {
      if (i > 0) {
        newScript += ", ";
      }

      newScript += paramNames[i];
    }

    newScript += ") { " + js + " };\n";
    return newScript;
  }

  /**
   * Create the __defineStatic method.
   *
   * @param logger
   */
  protected abstract void createStaticDispatcher(TreeLogger logger);

  /**
   * Invokes a native JavaScript function.
   *
   * @param name the name of the function to invoke
   * @param jthis the function's 'this' context
   * @param types the type of each argument
   * @param args the arguments to be passed
   * @return the return value as a Variant.
   */
  protected abstract JsValue doInvoke(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable;

  protected CompilingClassLoader getIsolatedClassLoader() {
    return host.getClassLoader();
  }

  /**
   * Injects the magic needed to resolve JSNI references from module-space.
   */
  protected abstract Object getStaticDispatcher();

  /**
   * Invokes a native JavaScript function.
   *
   * @param name the name of the function to invoke
   * @param jthis the function's 'this' context
   * @param types the type of each argument
   * @param args the arguments to be passed
   * @return the return value as a Variant.
   */
  protected final JsValue invokeNative(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    JsValue result = doInvoke(name, jthis, types, args);
    // Is an exception active?
    Throwable thrown = sCaughtJavaExceptionObject.get();
    if (thrown == null) {
      return result;
    }
    sCaughtJavaExceptionObject.set(null);

    scrubStackTrace(thrown);
    throw thrown;
  }

  /**
   * @param original the thrown exception
   * @param exception the caught exception
   */
  protected boolean isExceptionSame(Throwable original, Object exception) {
    // For most platforms, the null exception means we threw it.
    // IE overrides this.
    return exception == null;
  }

  protected String rebind(String sourceName) throws UnableToCompleteException {
    try {
      String result = host.rebind(logger, sourceName);
      if (result != null) {
        return result;
      } else {
        return sourceName;
      }
    } catch (UnableToCompleteException e) {
      String msg = "Deferred binding failed for '" + sourceName
          + "'; expect subsequent failures";
      host.getLogger().log(TreeLogger.ERROR, msg);
      throw e;
    }
  }

  private String composeResultErrorMsgPrefix(String name, String typePhrase) {
    return "Something other than " + typePhrase
        + " was returned from JSNI method '" + name + "'";
  }

  private void displayErrorGlassPanel(
      String summary, String entryPointTypeName, Throwable e) throws Throwable {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    String stackTrace = Util.escapeXml(writer.toString()).replaceFirst(
        // (?ms) for regex pattern modifiers MULTILINE and DOTALL
        "(?ms)(Caused by:.+)", "<b>$1</b>");
    String details = "<p>Exception while loading module <b>"
        + Util.escapeXml(entryPointTypeName) + "</b>."
        + " See Development Mode for details.</p>"
        + "<div style='overflow:visible;white-space:pre;'>" + stackTrace
        + "</div>";

    invokeNativeVoid("__gwt_displayGlassMessage", null,
        new Class[] { String.class, String.class },
        new Object[] { Util.escapeXml(summary), details });
  }

  private boolean isUserFrame(StackTraceElement element) {
    try {
      CompilingClassLoader cl = getIsolatedClassLoader();
      String className = element.getClassName();
      Class<?> clazz = Class.forName(className, false, cl);
      if (clazz.getClassLoader() == cl) {
        // Lives in user classLoader.
        return true;
      }
      // At this point, it must be a JRE class to qualify.
      if (clazz.getClassLoader() != null || !className.startsWith("java.")) {
        return false;
      }
      if (className.startsWith("java.lang.reflect.")) {
        return false;
      }
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Handles loading a class that might be nested given a source type name.
   */
  private Class<?> loadClassFromSourceName(String sourceName)
      throws ClassNotFoundException {
    Event moduleSpaceClassLoad = SpeedTracerLogger.start(
        DevModeEventType.MODULE_SPACE_CLASS_LOAD, "Source Name", sourceName);
    try {
      String toTry = sourceName;
      while (true) {
        try {
          return Class.forName(toTry, true, getIsolatedClassLoader());
        } catch (ClassNotFoundException e) {
          // Assume that the last '.' should be '$' and try again.
          //
          int i = toTry.lastIndexOf('.');
          if (i == -1) {
            throw e;
          }

          toTry = toTry.substring(0, i) + "$" + toTry.substring(i + 1);
        }
      }
    } finally {
      moduleSpaceClassLoad.end();
    }
  }

  /**
   * Clean up the stack trace by removing our hosting frames. But don't do this
   * if our own frames are at the top of the stack, because we may be the real
   * cause of the exception.
   */
  private void scrubStackTrace(Throwable thrown) {
    List<StackTraceElement> trace = new ArrayList<StackTraceElement>(
        Arrays.asList(thrown.getStackTrace()));
    boolean seenUserFrame = false;
    for (ListIterator<StackTraceElement> it = trace.listIterator(); it.hasNext();) {
      StackTraceElement element = it.next();
      if (!isUserFrame(element)) {
        if (seenUserFrame) {
          it.remove();
        }
        continue;
      }
      seenUserFrame = true;

      // Remove a JavaScriptHost.invokeNative*() frame.
      if (element.getClassName().equals(JavaScriptHost.class.getName())) {
        if (element.getMethodName().equals("exceptionCaught")) {
          it.remove();
        } else if (element.getMethodName().startsWith("invokeNative")) {
          it.remove();
          // Also try to convert the next frame to a true native.
          if (it.hasNext()) {
            StackTraceElement next = it.next();
            if (next.getLineNumber() == -1) {
              next = new StackTraceElement(next.getClassName(),
                  next.getMethodName(), next.getFileName(), -2);
              it.set(next);
            }
          }
        }
      }
    }
    thrown.setStackTrace(trace.toArray(new StackTraceElement[trace.size()]));
  }
}
