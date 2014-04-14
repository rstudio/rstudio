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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.collect.Maps;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

/**
 * Creates proxies for annotation objects that...
 */
class AnnotationProxyFactory {
  /**
   * {@link InvocationHandler} implementation used by all
   * {@link java.lang.annotation.Annotation Annotation} proxies created by the
   * {@link TypeOracle}.
   */
  private static class AnnotationProxyInvocationHandler implements
      InvocationHandler {

    /**
     * Returns <code>true</code> if the expected return type is assignable
     * from the actual return type or if the expected return type is a primitive
     * and the actual return type is the corresponding wrapper type.
     */
    private static boolean isValidReturnType(Class<?> expectedReturnType,
        Class<? extends Object> actualReturnType) {
      if (expectedReturnType.isAssignableFrom(actualReturnType)) {
        return true;
      }

      if (expectedReturnType.isPrimitive()) {
        if (expectedReturnType == boolean.class) {
          return actualReturnType == Boolean.class;
        } else if (expectedReturnType == byte.class) {
          return actualReturnType == Byte.class;
        } else if (expectedReturnType == char.class) {
          return actualReturnType == Character.class;
        } else if (expectedReturnType == double.class) {
          return actualReturnType == Double.class;
        } else if (expectedReturnType == float.class) {
          return actualReturnType == Float.class;
        } else if (expectedReturnType == int.class) {
          return actualReturnType == Integer.class;
        } else if (expectedReturnType == long.class) {
          return actualReturnType == Long.class;
        } else if (expectedReturnType == short.class) {
          return actualReturnType == Short.class;
        }
      }

      return false;
    }

    /**
     * The resolved class of this annotation.
     */
    private Class<? extends Annotation> annotationClass;

    /**
     * Maps method names onto values. Note that methods on annotation types
     * cannot be overloaded because they have zero arguments.
     */
    private final Map<String, Object> identifierToValue;

    /**
     * A reference to the enclosing proxy object.
     */
    private Annotation proxy;

    public AnnotationProxyInvocationHandler(
        Map<String, Object> identifierToValue,
        Class<? extends Annotation> annotationClass) {
      this.identifierToValue = Maps.normalizeUnmodifiable(identifierToValue);
      this.annotationClass = annotationClass;
    }

    @Override
    public boolean equals(Object other) {
      // This is not actually an asymmetric equals implementation, as this
      // method gets called for our proxy instance rather than on the handler
      // itself.
      if (proxy == other) {
        return true;
      }

      if (!(other instanceof Annotation)) {
        return false;
      }

      Annotation otherAnnotation = (Annotation) other;
      if (annotationClass != otherAnnotation.annotationType()) {
        return false;
      }

      try {
        for (Method method : annotationClass.getDeclaredMethods()) {
          Object myVal = method.invoke(proxy);
          Object otherVal = method.invoke(other);

          if (myVal instanceof Object[]) {
            if (!Arrays.equals((Object[]) myVal, (Object[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof boolean[]) {
            if (!Arrays.equals((boolean[]) myVal, (boolean[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof byte[]) {
            if (!Arrays.equals((byte[]) myVal, (byte[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof char[]) {
            if (!Arrays.equals((char[]) myVal, (char[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof short[]) {
            if (!Arrays.equals((short[]) myVal, (short[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof int[]) {
            if (!Arrays.equals((int[]) myVal, (int[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof long[]) {
            if (!Arrays.equals((long[]) myVal, (long[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof float[]) {
            if (!Arrays.equals((float[]) myVal, (float[]) otherVal)) {
              return false;
            }
          } else if (myVal instanceof double[]) {
            if (!Arrays.equals((double[]) myVal, (double[]) otherVal)) {
              return false;
            }
          } else {
            if (!myVal.equals(otherVal)) {
              return false;
            }
          }
        }
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getTargetException());
      }
      return true;
    }

    @Override
    public int hashCode() {
      int sum = 0;
      try {
        for (Method method : annotationClass.getDeclaredMethods()) {
          Object myVal = method.invoke(proxy);
          int memberHash;
          if (myVal instanceof Object[]) {
            memberHash = Arrays.hashCode((Object[]) myVal);
          } else if (myVal instanceof boolean[]) {
            memberHash = Arrays.hashCode((boolean[]) myVal);
          } else if (myVal instanceof byte[]) {
            memberHash = Arrays.hashCode((byte[]) myVal);
          } else if (myVal instanceof char[]) {
            memberHash = Arrays.hashCode((char[]) myVal);
          } else if (myVal instanceof short[]) {
            memberHash = Arrays.hashCode((short[]) myVal);
          } else if (myVal instanceof int[]) {
            memberHash = Arrays.hashCode((int[]) myVal);
          } else if (myVal instanceof long[]) {
            memberHash = Arrays.hashCode((long[]) myVal);
          } else if (myVal instanceof float[]) {
            memberHash = Arrays.hashCode((float[]) myVal);
          } else if (myVal instanceof double[]) {
            memberHash = Arrays.hashCode((double[]) myVal);
          } else {
            memberHash = myVal.hashCode();
          }
          // See doc for Annotation.hashCode.
          memberHash ^= 127 * method.getName().hashCode();
          sum += memberHash;
        }
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getTargetException());
      }
      return sum;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {

      Object value = null;
      if (args == null || args.length == 0) {
        // A no-arg method, try to process as an annotation method.
        String name = method.getName();
        if (identifierToValue.containsKey(name)) {
          // The value was explicitly provided
          value = identifierToValue.get(name);
          assert (value != null);
        } else {
          if ("annotationType".equals(method.getName())) {
            value = annotationClass;
          } else {
            value = method.getDefaultValue();
          }
        }
        if (value != null) {
          assert (isValidReturnType(method.getReturnType(), value.getClass()));
          return value;
        }
      }

      /*
       * Maybe it's an Object method, just delegate to myself.
       */
      return method.invoke(this, args);
    }

    public void setProxy(Annotation proxy) {
      this.proxy = proxy;
    }

    @Override
    public String toString() {
      final StringBuilder msg = new StringBuilder();
      String qualifiedSourceName = annotationClass.getName().replace('$', '.');
      msg.append('@').append(qualifiedSourceName).append('(');
      boolean first = true;
      try {
        for (Method method : annotationClass.getDeclaredMethods()) {
          if (first) {
            first = false;
          } else {
            msg.append(", ");
          }
          msg.append(method.getName()).append('=');
          Object myVal = method.invoke(proxy);
          if (myVal.getClass().isArray()) {
            msg.append(java.util.Arrays.deepToString((Object[]) myVal));
          } else {
            msg.append(myVal);
          }
        }
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getTargetException());
      }
      msg.append(')');
      return msg.toString();
    }
  }

  public static Annotation create(Class<? extends Annotation> annotationClass,
      Map<String, Object> identifierToValue) {
    AnnotationProxyInvocationHandler annotationInvocationHandler = new AnnotationProxyInvocationHandler(
        identifierToValue, annotationClass);
    Annotation proxy = (Annotation) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(), new Class<?>[] {
            java.lang.annotation.Annotation.class, annotationClass},
        annotationInvocationHandler);
    annotationInvocationHandler.setProxy(proxy);
    return proxy;
  }
}