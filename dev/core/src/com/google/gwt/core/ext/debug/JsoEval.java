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
package com.google.gwt.core.ext.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides facilities for debuggers to call methods on
 * {@link com.google.gwt.core.client.JavaScriptObject JavaScriptObjects}.
 * <p/>
 * Because devmode does extensive rewriting of JSO bytecode, debuggers can't
 * figure out how to evaluate JSO method calls. This class can be used directly
 * by users to evaluate JSO methods in their debuggers. Additionally, debuggers
 * with GWT support use this class to transparently evaluate JSO expressions in
 * breakpoints, watch windows, etc.
 * <p>
 * Example uses:
 * <code><pre>
 *   JsoEval.call(Element.class, myElement, "getAbsoluteTop");
 *   JsoEval.call(Node.class, myNode, "cloneNode", Boolean.TRUE);
 *   JsoEval.call(Element.class, element.getFirstChildElement(), "setPropertyString", "phase",
 *     "gamma");
 * </pre></code>
 */
public class JsoEval {

  /* TODO: Error messages generated from JsoEval are reported with mangled
   * method names and signatures instead of original source code values.
   * We could de-mangle the names for the errors, but it really only matters
   * for users who don't have IDE support.
   */

  // TODO: Update the wiki doc to include a better description of JSO transformations and reference
  // it from here.

  private static Map<Class,Class> boxedTypeForPrimitiveType = new HashMap<Class,Class>(8);
  private static Map<Class,Class> primitiveTypeForBoxedType = new HashMap<Class,Class>(8);

  private static final String JSO_IMPL_CLASS = "com.google.gwt.core.client.JavaScriptObject$";

  static {
    boxedTypeForPrimitiveType.put(boolean.class, Boolean.class);
    boxedTypeForPrimitiveType.put(byte.class, Byte.class);
    boxedTypeForPrimitiveType.put(short.class, Short.class);
    boxedTypeForPrimitiveType.put(char.class, Character.class);
    boxedTypeForPrimitiveType.put(int.class, Integer.class);
    boxedTypeForPrimitiveType.put(float.class, Float.class);
    boxedTypeForPrimitiveType.put(long.class, Long.class);
    boxedTypeForPrimitiveType.put(double.class, Double.class);

    for (Map.Entry<Class,Class> entry : boxedTypeForPrimitiveType.entrySet()) {
      primitiveTypeForBoxedType.put(entry.getValue(), entry.getKey());
    }
  }

  /**
   * Reflectively invokes a method on a JavaScriptObject.
   *
   * @param klass Either a class of type JavaScriptObject or an interface
   * implemented by a JavaScriptObject. The class must contain the method to
   * be invoked.
   * @param obj The JavaScriptObject to invoke the method on. Must be null if
   * the method is static. Must be not-null if the method is not static
   * @param methodName The name of the method
   * @param types The types of the arguments
   * @param args The values of the arguments
   *
   * @return The result of the method invocation or the failure as a String
   */
  public static Object call(Class klass, Object obj, String methodName, Class[] types,
      Object... args) {
    try {
      return callEx(klass, obj, methodName, types, args);
    } catch (Exception e) {
      return toString(e);
    }
  }

  /**
   * A convenience form of
   * {@link #call(Class, Object, String, Class[], Object...)} for use directly
   * by users in a debugger. This method guesses at the types of the method
   * based on the values of {@code args}.
   *
   * @return The result of the method invocation or the failure as a String
   */
  public static Object call(Class klass, Object obj, String methodName, Object... args) {
    try {
      return callEx(klass, obj, methodName, args);
    } catch (Exception e) {
      return toString(e);
    }
  }

  /**
   * Reflectively invokes a method on a JavaScriptObject.
   *
   * @param klass Either a class of type JavaScriptObject or an interface
   * implemented by a JavaScriptObject. The class must contain the method to
   * be invoked.
   * @param obj The JavaScriptObject to invoke the method on. Must be null if
   * the method is static. Must be not-null if the method is not static
   * @param methodName The name of the method
   * @param types The types of the arguments
   * @param args The values of the arguments
   *
   * @return The result of the method invocation
   */
  public static Object callEx(Class klass, Object obj, String methodName, Class[] types,
      Object... args)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      IllegalAccessException {
    return invoke(klass, obj, getJsoMethod(klass, obj, methodName, types), args);
  }

  /**
   * A convenience form of
   * {@link #call(Class, Object, String, Class[], Object...)} for use directly
   * by users in a debugger. This method guesses at the types of the method
   * based on the values of {@code args}.
   */
  public static Object callEx(Class klass, Object obj, String methodName, Object... args)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      IllegalAccessException {
    if (args == null) {
      // A single-argument varargs null can come in unboxed
      args = new Object[]{null};
    }

    if (obj != null) {
      if (!obj.getClass().getName().equals(JSO_IMPL_CLASS)) {
        throw new RuntimeException(obj + " is not a JavaScriptObject.");
      }
    }

    // First check java.lang.Object methods for exact matches
    Method[] methods = Object.class.getMethods();
    nextMethod: for (Method m : methods) {
      if (m.getName().equals(methodName)) {
        Class[] types = m.getParameterTypes();
        if (types.length != args.length) {
          continue;
        }
        for (int i = 0, j = 0; i < args.length; ++i, ++j) {
          if (!isAssignable(types[i], args[j])) {
            continue nextMethod;
          }
        }
        return m.invoke(obj, args);
      }
    }

    ClassLoader ccl = getCompilingClassLoader(klass, obj);
    boolean isJso = isJso(ccl, klass);
    boolean isStaticifiedDispatch = isJso && obj != null;
    int actualNumArgs = isStaticifiedDispatch ? args.length + 1 : args.length;

    ArrayList<Method> matchingMethods = new ArrayList<Method>(Arrays.asList(
        isJso ? getSisterJsoImpl(klass, ccl).getMethods() : getJsoImplClass(ccl).getMethods()));

    String mangledMethodName = mangleMethod(klass, methodName, isJso, isStaticifiedDispatch);

    // Filter the methods in multiple passes to give better error messages.
    for (Iterator<Method> it = matchingMethods.iterator(); it.hasNext();) {
      Method m = it.next();
      if (!m.getName().equalsIgnoreCase(mangledMethodName)) {
        it.remove();
      }
    }

    if (matchingMethods.isEmpty()) {
      throw new RuntimeException(
          "No methods by the name, " + methodName + ", could be found in " + klass);
    }

    ArrayList<Method> candidates = new ArrayList<Method>(matchingMethods);

    for (Iterator<Method> it = matchingMethods.iterator(); it.hasNext();) {
      Method m = it.next();
      if (m.getParameterTypes().length != actualNumArgs) {
        it.remove();
      }
    }

    if (matchingMethods.isEmpty()) {
      throw new RuntimeException(
          "No methods by the name, " + methodName + ", in " + klass + " accept "
              + args.length + " parameters. Candidates are:\n" + candidates);
    }

    candidates = new ArrayList<Method>(matchingMethods);

    nextMethod: for (Iterator<Method> it = matchingMethods.iterator(); it.hasNext();) {
      Method m = it.next();
      Class[] methodTypes = m.getParameterTypes();
      for (int i = isStaticifiedDispatch ? 1 : 0, j = 0; i < methodTypes.length; ++i, ++j) {
        if (!isAssignable(methodTypes[i], args[j])) {
          it.remove();
          continue nextMethod;
        }
      }
    }

    if (matchingMethods.isEmpty()) {
      throw new RuntimeException(
          "No methods accepting " + Arrays.asList(args) + " were found for, " + methodName
              + ", in " + klass + ". Candidates:\n" + candidates);
    }

    candidates = new ArrayList<Method>(matchingMethods);

    if (matchingMethods.size() > 1) {
      // Try to filter by exact name on the crazy off chance there are two
      // methods by same name but different case.
      for (Iterator<Method> it = matchingMethods.iterator(); it.hasNext();) {
        Method m = it.next();
        if (!m.getName().equals(mangledMethodName)) {
          it.remove();
        }
      }
    }

    if (matchingMethods.isEmpty()) {
      throw new RuntimeException(
          "Multiple methods with a case-insensitive match were found for, " + methodName
              + ", in " + klass + ". Candidates:\n" + candidates);
    }

    if (matchingMethods.size() > 1) {
      throw new RuntimeException(
          "Found more than one matching method. Please specify the types of the parameters. "
              + "Candidates:\n" + matchingMethods);
    }

    return invoke(klass, obj, matchingMethods.get(0), args);
  }

  /**
   * Reflectively invokes a static method on a JavaScriptObject. Has the same
   * effect as calling {@link #call(Class, Object, String, Class[], Object...)
   * call(klass, null, methodName, types, args)}
   *
   * @return The result of the method invocation or the failure as a String
   */
  public static Object callStatic(Class klass, String methodName, Class[] types, Object... args) {
    try {
      return callStaticEx(klass, methodName, types, args);
    } catch (Exception e) {
      return toString(e);
    }
  }

  /**
   * Reflectively invokes a static method on a JavaScriptObject. Has the same
   * effect as calling {@link #call(Class, Object, String, Class[], Object...)
   * call(klass, null, methodName, types, args)}
   */
  public static Object callStaticEx(Class klass, String methodName, Class[] types, Object... args)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      IllegalAccessException {
    return call(klass, null, methodName, types, args);
  }

  /**
   * Try to find the CompilingClassLoader. This can fail if<ol>
   * <li> the user provides an object that isn't a JSO or
   * <li>the user provides a null JSO and a Class that wasn't loaded by the
   * CompilingClassLoader
   * </ol>
   * I don't have any great solutions for that scenario.
   */
  private static ClassLoader getCompilingClassLoader(Class klass, Object obj) {
    ClassLoader ccl;

    if (obj != null) {
      ccl = obj.getClass().getClassLoader();
    } else {
      // try passed in class
      ccl = klass.getClassLoader();
    }

    if (ccl == null ||
        !ccl.getClass().getName().equals("com.google.gwt.dev.shell.CompilingClassLoader")) {
      if (obj != null) {
        throw new RuntimeException(
            "The object, " + obj + ", does not appear to be a JavaScriptObject or an interface " +
                "implemented by a JavaScriptObject. GWT could not find a CompilingClassLoader " +
                "for it.");
      } else {
        throw new RuntimeException(
            "The class, " + klass + ", does not appear to be a JavaScriptObject or an interface " +
                "implemented by a JavaScriptObject. GWT could not find a CompilingClassLoader " +
                " for it.");
      }
    }
    return ccl;
  }

  /**
   * Returns the class for {@code JavaScriptObject}. We need the version which
   * is loaded by a specific CompilingClassLoader.
   */
  private static Class getJsoClass(ClassLoader cl) {
    try {
      return Class.forName("com.google.gwt.core.client.JavaScriptObject", false, cl);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed to find JavaScriptObject", e);
    }
  }

  /**
   * Returns the class for {@code JavaScriptObject$}. We need the version which
   * is loaded by a specific CompilingClassLoader.
   */
  private static Class getJsoImplClass(ClassLoader cl) {
    try {
      return Class.forName(JSO_IMPL_CLASS, false, cl);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed to find " + JSO_IMPL_CLASS, e);
    }
  }

  private static Method getJsoMethod(Class klass, Object obj, String methodName, Class[] types)
      throws ClassNotFoundException, NoSuchMethodException {
    if (obj != null) {
      if (!obj.getClass().getName().equals(JSO_IMPL_CLASS)) {
        throw new RuntimeException(obj + " is not a JavaScriptObject.");
      }
    }

    // First see if it's a method inherited from java.lang.Object
    Method[] methods = Object.class.getMethods();
    for (Method m : methods) {
      if (m.getName().equals(methodName) && Arrays.equals(m.getParameterTypes(), types)) {
        return m;
      }
    }

    ClassLoader ccl = getCompilingClassLoader(klass, obj);
    boolean isJso = isJso(ccl, klass);
    boolean isStaticifiedDispatch = isJso && obj != null;
    String mangledMethod = mangleMethod(klass, methodName, isJso, isStaticifiedDispatch);

    if (!isJso) {
      // If this is interface dispatch, then the method lives on
      // JavaScriptObject$ and is mangled so that it doesn't conflict with any
      // other classes.
      Class jsoImplClass = getJsoImplClass(ccl);
      try {
        return jsoImplClass.getMethod(mangledMethod, types);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Unable to find the interface method, " + methodName
            + ". Is there a JSO that implements it?", e);
      }
    }

    // All other methods lives on the impl subclass of JavaScriptObject$,
    // and have been rewritten to be static dispatch.
    Class jsoImplSubclass = getSisterJsoImpl(klass, ccl);

    if (obj != null) {
      // If this is an instance method, we need to insert obj as the "this" ref
      // in the args
      Class[] newTypes = new Class[types.length + 1];
      newTypes[0] = klass;
      System.arraycopy(types, 0, newTypes, 1, types.length);
      types = newTypes;
    }

    return jsoImplSubclass.getMethod(mangledMethod, types);
  }

  private static Class<?> getSisterJsoImpl(Class klass, ClassLoader ccl)
      throws ClassNotFoundException {
    return Class.forName(klass.getName() + '$', false, ccl);
  }

  private static Object invoke(Class klass, Object obj, Method m, Object... args)
      throws InvocationTargetException, IllegalAccessException, ClassNotFoundException,
      NoSuchMethodException {
    if (args == null) {
      // A single-argument varargs null can come in unboxed
      args = new Object[]{null};
    }

    ClassLoader ccl = getCompilingClassLoader(klass, obj);

    if (!isJso(ccl, klass)) {
      // Calling through a non-JSO interface - normal instance dispatch.
      Object result = m.invoke(obj, args);
      return m.getReturnType() == void.class ? "[success]" : result;
    }

    // All other methods lives on the impl subclass of JavaScriptObject$,
    // and have been rewritten to be static dispatch.
    if (obj != null) {
      // If this is an instance method, we need to insert obj as the "this"
      // ref in the args
      Object[] newArgs = new Object[args.length + 1];
      newArgs[0] = obj;
      System.arraycopy(args, 0, newArgs, 1, args.length);
      args = newArgs;
    }

    Object result = m.invoke(obj, args);
    return m.getReturnType() == void.class ? "[success]" : result;
  }

  private static boolean isAssignable(Class type, Object value) {
    if (value == null) {
      return !type.isPrimitive();
    }
    Class valueType = value.getClass();
    if (type.isAssignableFrom(valueType)) {
      return true;
    }
    if (boxedTypeForPrimitiveType.get(valueType) == type
        || primitiveTypeForBoxedType.get(valueType) == type) {
      return true;
    }
    return false;
  }

  private static boolean isJso(ClassLoader ccl, Class klass) {
    return getJsoClass(ccl).isAssignableFrom(klass);
  }

  private static String mangleMethod(Class klass, String methodName, boolean isJso,
      boolean isVirtual) {
    // If this is interface dispatch from a non-JSO, then the method lives on
    // JavaScriptObject$ and is mangled with the fully qualified class name so
    // that it doesn't conflict with methods from other classes. Otherwise
    // virtual dispatch is re-written to static dispatch, and a '$' is
    // appended to the name of the method.
    return isJso ? isVirtual ? methodName + '$' : methodName
        : klass.getName().replace('.', '_') + '_' + methodName;
  }

  private static String toString(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter w = new PrintWriter(sw);
    e.printStackTrace(w);
    w.close();
    return sw.toString();
  }

  private JsoEval() {
  }
}
