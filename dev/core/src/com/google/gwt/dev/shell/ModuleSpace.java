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

  protected static ThreadLocal sCaughtJavaExceptionObject = new ThreadLocal();
  protected static ThreadLocal sThrownJavaExceptionObject = new ThreadLocal();

  /**
   * Logger is thread local.
   */
  private static ThreadLocal threadLocalLogger = new ThreadLocal();

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
      Constructor ctor = javaScriptExceptionClass.getDeclaredConstructor(new Class[] {
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
    return (TreeLogger) threadLocalLogger.get();
  }

  private final ModuleSpaceHost host;

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

  public void ditchHandle(int opaque) {
    Handle.enqueuePtr(opaque);
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
    initializeStaticDispatcher();

    // Actually run user code.
    //
    String entryPointTypeName = null;
    try {
      String[] entryPoints = host.getEntryPointTypeNames();
      if (entryPoints.length > 0) {
        for (int i = 0; i < entryPoints.length; i++) {
          entryPointTypeName = entryPoints[i];
          Class clazz = loadClassFromSourceName(entryPointTypeName);
          Method onModuleLoad = null;
          try {
            onModuleLoad = clazz.getMethod("onModuleLoad", null);
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
            onModuleLoad = module.getClass().getMethod("onModuleLoad", null);
          }
          onModuleLoad.setAccessible(true);
          onModuleLoad.invoke(module, null);
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

      final String unableToLoadMessage = "Unable to load module entry point class "
          + entryPointTypeName;
      logger.log(TreeLogger.ERROR, unableToLoadMessage, caught);
      throw new UnableToCompleteException();
    }
  }

  public Object rebindAndCreate(String requestedClassName)
      throws UnableToCompleteException {
    Throwable caught = null;
    String msg = null;
    String resultName = null;
    try {
      // Rebind operates on source-level names.
      //
      String sourceName = requestedClassName.replace('$', '.');
      resultName = rebind(sourceName);
      Class resolvedClass = loadClassFromSourceName(resultName);
      if (Modifier.isAbstract(resolvedClass.getModifiers())) {
        msg = "Deferred binding result type '" + resultName
            + "' should not be abstract";
      } else {
        Constructor ctor = resolvedClass.getDeclaredConstructor(null);
        ctor.setAccessible(true);
        return ctor.newInstance(null);
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

  protected CompilingClassLoader getIsolatedClassLoader() {
    return host.getClassLoader();
  }

  /**
   * Injects the magic needed to resolve JSNI references from module-space.
   */
  protected abstract void initializeStaticDispatcher();

  protected boolean isExceptionActive() {
    return sCaughtJavaExceptionObject.get() != null;
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

  protected RuntimeException takeJavaException() {
    RuntimeException re = (RuntimeException) sCaughtJavaExceptionObject.get();
    sCaughtJavaExceptionObject.set(null);
    return re;
  }

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
      final Class[] paramTypes = new Class[] {ShellJavaScriptHost.class};
      Method setHostMethod = jsHostClass.getMethod("setHost", paramTypes);
      setHostMethod.invoke(jsHostClass, new Object[] {null});
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
      final Class[] paramTypes = new Class[] {ShellJavaScriptHost.class};
      Method setHostMethod = jsHostClass.getMethod("setHost", paramTypes);
      setHostMethod.invoke(jsHostClass, new Object[] {this});
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
}
