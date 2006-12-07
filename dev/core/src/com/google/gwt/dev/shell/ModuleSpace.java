// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The interface to the low-level browser, this class serves as a 'domain' for a
 * module, loading all of its classes in a separate, isolated class loader. This
 * allows us to run multiple modules, both in succession and simultaneously.
 */
public abstract class ModuleSpace implements ShellJavaScriptHost {

  private final ModuleSpaceHost host;

  /**
   * Logger is thread local.
   */
  private static ThreadLocal threadLocalLogger = new ThreadLocal();

  protected ModuleSpace(final ModuleSpaceHost host) {
    this.host = host;
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
    initializeStaticDispatcher();

    // Actually run user code.
    //
    String entryPointTypeName = null;
    try {
      String[] entryPoints = host.getEntryPointTypeNames();
      if (entryPoints.length > 0) {
        for (int i = 0; i < entryPoints.length; i++) {
          entryPointTypeName = entryPoints[i];
          Object module = rebindAndCreate(entryPointTypeName);
          Method onModuleLoad = module.getClass().getMethod("onModuleLoad",
            null);
          onModuleLoad.invoke(module, null);
        }
      } else {
        logger
          .log(
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

      final String UNABLE_TO_LOAD_MESSAGE = "Unable to load module entry point class "
        + entryPointTypeName;
      logger.log(TreeLogger.ERROR, UNABLE_TO_LOAD_MESSAGE, caught);
      throw new UnableToCompleteException();
    }
  }

  public Object rebindAndCreate(String requestedClassName)
      throws UnableToCompleteException {
    Throwable caught = null;
    try {
      // Rebind operates on source-level names.
      //
      String sourceName = requestedClassName.replace('$', '.');
      String resultName = rebind(sourceName);
      Class resolvedClass = loadClassFromSourceName(resultName);
      Constructor ctor = resolvedClass.getDeclaredConstructor(null);
      ctor.setAccessible(true);
      return ctor.newInstance(null);
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (ExceptionInInitializerError e) {
      caught = e.getException();
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }

    // Always log here because sometimes this method gets called from static
    // initializers and other unusual places, which can obscure the problem.
    //
    String msg = "Failed to create an instance of '" + requestedClassName
      + "' via deferred binding ";
    host.getLogger().log(TreeLogger.ERROR, msg, caught);

    throw new UnableToCompleteException();
  }

  /**
   * Handles loading a class that might be nested given a source type name.
   */
  private Class loadClassFromSourceName(String sourceName)
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

  public void ditchHandle(int opaque) {
    Handle.enqueuePtr(opaque);
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
   * Injects the magic needed to resolve JSNI references from module-space.
   */
  protected abstract void initializeStaticDispatcher();

  /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to be null.
   * 
   * @see JavaScriptHost
   */
  private void clearJavaScriptHost() {
    // Find the application's JavaScriptHost interface.
    //
    Throwable caught;
    try {
      final String jsHostClassName = JavaScriptHost.class.getName();
      Class jsHostClass = Class.forName(jsHostClassName, true,
        getIsolatedClassLoader());
      final Class[] paramTypes = new Class[]{ShellJavaScriptHost.class};
      Method setHostMethod = jsHostClass.getMethod("setHost", paramTypes);
      setHostMethod.invoke(jsHostClass, new Object[]{null});
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
    throw new RuntimeException("Error unintializing JavaScriptHost", caught);
  }

  /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to be this ModuleSpace instance.
   * 
   * @see JavaScriptHost
   */
  private void setJavaScriptHost() {
    // Find the application's JavaScriptHost interface.
    //
    Throwable caught;
    try {
      final String jsHostClassName = JavaScriptHost.class.getName();
      Class jsHostClass = Class.forName(jsHostClassName, true,
        getIsolatedClassLoader());
      final Class[] paramTypes = new Class[]{ShellJavaScriptHost.class};
      Method setHostMethod = jsHostClass.getMethod("setHost", paramTypes);
      setHostMethod.invoke(jsHostClass, new Object[]{this});
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
    throw new RuntimeException("Error intializing JavaScriptHost", caught);
  }

  protected static ThreadLocal sThrownJavaExceptionObject = new ThreadLocal();
  protected static ThreadLocal sCaughtJavaExceptionObject = new ThreadLocal();

  protected boolean isExceptionActive() {
    return sCaughtJavaExceptionObject.get() != null;
  }

  protected RuntimeException takeJavaException() {
    RuntimeException re = (RuntimeException) sCaughtJavaExceptionObject.get();
    sCaughtJavaExceptionObject.set(null);
    return re;
  }

  public static void setThrownJavaException(RuntimeException re) {
    getLogger().log(TreeLogger.WARN, "Exception thrown into JavaScript", re);
    sThrownJavaExceptionObject.set(re);
  }

  protected static RuntimeException createJavaScriptException(ClassLoader cl,
      String name, String desc) {
    Exception caught;
    try {
      Class javaScriptExceptionClass = Class.forName(
        "com.google.gwt.core.client.JavaScriptException", true, cl);
      Class string = String.class;
      Constructor ctor = javaScriptExceptionClass
        .getDeclaredConstructor(new Class[]{string, string});
      return (RuntimeException) ctor.newInstance(new Object[]{name, desc});
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

  protected CompilingClassLoader getIsolatedClassLoader() {
    return host.getClassLoader();
  }

  protected static TreeLogger getLogger() {
    return (TreeLogger) threadLocalLogger.get();
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
}
