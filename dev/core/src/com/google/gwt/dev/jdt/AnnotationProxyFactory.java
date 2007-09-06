/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.typeinfo.JAnnotationMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

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
     * The resolved class of this annotation.
     */
    private Class<? extends Annotation> annotationClass;

    private final JClassType annotationType;

    /**
     * Maps method names onto values. Note that methods on annotation types
     * cannot be overloaded because they have zero arguments.
     */
    private final Map<String, Object> identifierToValue;

    /**
     * A reference to the enclosing proxy object.
     */
    private Annotation proxy;

    public AnnotationProxyInvocationHandler(JClassType annotationType,
        Map<String, Object> identifierToValue,
        Class<? extends Annotation> annotationClass) {
      this.annotationType = annotationType;
      this.identifierToValue = identifierToValue;
      this.annotationClass = annotationClass;
    }

    @Override
    public boolean equals(Object other) {
      if (proxy == other) {
        return true;
      }
      if (!annotationClass.isInstance(other)) {
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
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {

      String name = method.getName();

      // See if the value was explicitly declared
      Object value = identifierToValue.get(name);
      if (value != null) {
        return value;
      }

      // Try to find a method on the interface.
      JMethod jMethod = annotationType.findMethod(name, new JType[0]);
      if (jMethod != null) {
        JAnnotationMethod annotationMethod = jMethod.isAnnotationMethod();
        assert (annotationMethod != null);
        return annotationMethod.getDefaultValue();
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
      msg.append('@').append(annotationType.getQualifiedSourceName()).append(
          '(');
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
      JClassType annotationType, Map<String, Object> identifierToValue) {
    AnnotationProxyInvocationHandler annotationInvocationHandler = new AnnotationProxyInvocationHandler(
        annotationType, identifierToValue, annotationClass);
    Annotation proxy = (Annotation) Proxy.newProxyInstance(
        AnnotationProxyFactory.class.getClassLoader(), new Class<?>[] {
            java.lang.annotation.Annotation.class, annotationClass},
        annotationInvocationHandler);
    annotationInvocationHandler.setProxy(proxy);
    return proxy;
  }
}