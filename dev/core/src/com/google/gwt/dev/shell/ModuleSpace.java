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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

  protected static RuntimeException createJavaScriptException(ClassLoader cl,
      String name, String desc) {
    Exception caught;
    try {
      Class<?> javaScriptExceptionClass = Class.forName(
          "com.google.gwt.core.client.JavaScriptException", true, cl);
      Class<?> string = String.class;
      Constructor<?> ctor = javaScriptExceptionClass.getDeclaredConstructor(new Class<?>[] {
          string, string});
      return (RuntimeException) ctor.newInstance(new Object[] {name, desc});
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
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to be the specified ModuleSpace instance
   * (which will either be this ModuleSpace or null).
   * 
   * @param moduleSpace the ModuleSpace instance to store using
   *          JavaScriptHost.setHost().
   * @see JavaScriptHost
   */
  private static void setJavaScriptHost(ModuleSpace moduleSpace, ClassLoader cl) {
    // Find the application's JavaScriptHost interface.
    //
    Throwable caught;
    try {
      final String jsHostClassName = JavaScriptHost.class.getName();
      Class<?> jsHostClass = Class.forName(jsHostClassName, true, cl);
      final Class<?>[] paramTypes = new Class[] {ShellJavaScriptHost.class};
      Method setHostMethod = jsHostClass.getMethod("setHost", paramTypes);
      setHostMethod.invoke(jsHostClass, new Object[] {moduleSpace});
      return;
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }
    throw new RuntimeException("Error initializing JavaScriptHost", caught);
  }

  private final ModuleSpaceHost host;

  private final Object key;

  private final String moduleName;

  protected ModuleSpace(ModuleSpaceHost host, String moduleName, Object key) {
    this.host = host;
    this.moduleName = moduleName;
    this.key = key;
    TreeLogger hostLogger = host.getLogger();
    threadLocalLogger.set(hostLogger);
  }

  public void dispose() {
    // Tell the user-space JavaScript host object that we're done
    //
    clearJavaScriptHost();

    // Clear out the class loader's cache
    host.getClassLoader().clear();
  }

  public void exceptionCaught(int number, String name, String message) {
    Throwable thrown = sThrownJavaExceptionObject.get();

    if (thrown != null) {
      // See if the caught exception was thrown by us
      if (isExceptionSame(thrown, number, name, message)) {
        sCaughtJavaExceptionObject.set(thrown);
        sThrownJavaExceptionObject.set(null);
        return;
      }
    }

    sCaughtJavaExceptionObject.set(createJavaScriptException(
        getIsolatedClassLoader(), name, message));
  }

  /**
   * Get the unique key for this module.
   * 
   * @return the unique key
   */
  public Object getKey() {
    return key;
  }

  /**
   * Get the module name.
   * 
   * @return the module name
   */
  public String getModuleName() {
    return moduleName;
  }

  public boolean invokeNativeBoolean(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeBoolean(" + name + ")";
    Boolean value = JsValueGlue.get(result, getIsolatedClassLoader(),
        boolean.class, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a boolean");
    }
    return value.booleanValue();
  }

  public byte invokeNativeByte(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeByte(" + name + ")";
    Byte value = JsValueGlue.get(result, null, Byte.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a byte");
    }
    return value.byteValue();
  }

  public char invokeNativeChar(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeCharacter(" + name + ")";
    Character value = JsValueGlue.get(result, null, Character.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a char");
    }
    return value.charValue();
  }

  public double invokeNativeDouble(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeDouble(" + name + ")";
    Double value = JsValueGlue.get(result, null, Double.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a double");
    }
    return value.doubleValue();
  }

  public float invokeNativeFloat(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeFloat(" + name + ")";
    Float value = JsValueGlue.get(result, null, Float.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a float");
    }
    return value.floatValue();
  }

  public int invokeNativeInt(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeInteger(" + name + ")";
    Integer value = JsValueGlue.get(result, null, Integer.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected an int");
    }
    return value.intValue();
  }

  public long invokeNativeLong(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeLong(" + name + ")";
    Long value = JsValueGlue.get(result, null, Long.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a long");
    }
    return value.longValue();
  }

  public Object invokeNativeObject(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    return JsValueGlue.get(result, getIsolatedClassLoader(), Object.class,
        "invokeNativeObject(" + name + ")");
  }

  public short invokeNativeShort(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    String msgPrefix = "invokeNativeShort(" + name + ")";
    Short value = JsValueGlue.get(result, null, Short.TYPE, msgPrefix);
    if (value == null) {
      throw new HostedModeException(msgPrefix
          + ": return value null received, expected a short");
    }
    return value.shortValue();
  }

  public void invokeNativeVoid(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    JsValue result = invokeNative(name, jthis, types, args);
    if (!result.isUndefined()) {
      getLogger().log(
          TreeLogger.WARN,
          "JSNI method '" + name + "' returned a value of type "
              + result.getTypeString() + "; should not have returned a value",
          null);
    }
  }

  /**
   * Allows client-side code to log to the tree logger.
   */
  public void log(String message, Throwable e) {
    TreeLogger logger = host.getLogger();
    TreeLogger.Type type = TreeLogger.INFO;
    if (e != null) {
      type = TreeLogger.ERROR;
    }
    logger.log(type, message, e);
  }

  /**
   * Runs the module's user startup code.
   */
  public final void onLoad(TreeLogger logger) throws UnableToCompleteException {
    // Tell the host we're ready for business.
    //
    host.onModuleReady(this);

    // Tell the user-space JavaScript host object how to get back here.
    //
    setJavaScriptHost();

    // Make sure we can resolve JSNI references to static Java names.
    //
    try {
      Object staticDispatch = getStaticDispatcher();
      createNative("initializeStaticDispatcher", 0, "__defineStatic",
          new String[] {"__arg0"}, "window.__static = __arg0;");
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
      String[] entryPoints = host.getEntryPointTypeNames();
      if (entryPoints.length > 0) {
        for (int i = 0; i < entryPoints.length; i++) {
          entryPointTypeName = entryPoints[i];
          Class<?> clazz = loadClassFromSourceName(entryPointTypeName);
          Method onModuleLoad = null;
          try {
            onModuleLoad = clazz.getMethod("onModuleLoad");
            if (!Modifier.isStatic(onModuleLoad.getModifiers())) {
              // it's non-static, so we need to rebind the class
              onModuleLoad = null;
            }
          } catch (NoSuchMethodException e) {
            // okay, try rebinding it; maybe the rebind result will have one
          }
          Object module = null;
          if (onModuleLoad == null) {
            module = rebindAndCreate(entryPointTypeName);
            onModuleLoad = module.getClass().getMethod("onModuleLoad");
          }
          onModuleLoad.setAccessible(true);
          onModuleLoad.invoke(module);
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
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T rebindAndCreate(String requestedClassName)
      throws UnableToCompleteException {
    Throwable caught = null;
    String msg = null;
    String resultName = null;
    try {
      // Rebind operates on source-level names.
      //
      String sourceName = requestedClassName.replace('$', '.');
      resultName = rebind(sourceName);
      Class<?> resolvedClass = loadClassFromSourceName(resultName);
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
      msg = "Rebind result '" + resultName
          + "' has no default (zero argument) constructors.";
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
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
    // Whenever a native method is invoked, release any enqueued cleanup objects
    JsValue.mainThreadCleanup();
    JsValue result = doInvoke(name, jthis, types, args);
    // Is an exception active?
    Throwable thrown = sCaughtJavaExceptionObject.get();
    if (thrown == null) {
      return result;
    }
    sCaughtJavaExceptionObject.set(null);

    /*
     * The stack trace on the stored exception will not be very useful due to
     * how it was created. Using fillInStackTrace() resets the stack trace to
     * this moment in time, which is usually far more useful.
     */
    thrown.fillInStackTrace();
    throw thrown;
  }

  @SuppressWarnings("unused")
  protected boolean isExceptionSame(Throwable original, int number,
      String name, String message) {
    // For most platforms, the null exception means we threw it.
    // IE overrides this.
    return (name == null && message == null);
  }

  protected String rebind(String sourceName) throws UnableToCompleteException {
    try {
      String result = host.rebind(host.getLogger(), sourceName);
      if (result != null) {
        return result;
      } else {
        return sourceName;
      }
    } catch (UnableToCompleteException e) {
      String msg = "Deferred binding failed for '" + sourceName
          + "'; expect subsequent failures";
      host.getLogger().log(TreeLogger.ERROR, msg, e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Clear the module's JavaScriptHost 'host' field.
   */
  private void clearJavaScriptHost() {
    setJavaScriptHost(null, getIsolatedClassLoader());
  }

  /**
   * Handles loading a class that might be nested given a source type name.
   */
  private Class<?> loadClassFromSourceName(String sourceName)
      throws ClassNotFoundException {
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
  }

  /**
   * Set the module's JavaScriptHost 'host' field to this ModuleSpace instance.
   */
  private void setJavaScriptHost() {
    setJavaScriptHost(this, getIsolatedClassLoader());
  }

}
