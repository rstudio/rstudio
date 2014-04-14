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
import com.google.gwt.dev.util.TypeInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Glue layer that performs GWT-specific operations on JsValues. Used to isolate
 * HostedModeExceptions/etc from JsValue code
 */
public final class JsValueGlue {
  public static final String HOSTED_MODE_REFERENCE = "hostedModeReference";
  public static final String JSO_CLASS = "com.google.gwt.core.client.JavaScriptObject";
  public static final String JSO_IMPL_CLASS = "com.google.gwt.core.client.JavaScriptObject$";

  /**
   * Create a JavaScriptObject instance referring to this JavaScript object.
   *
   * @param classLoader the classLoader to create from
   * @return the constructed JavaScriptObject
   */
  public static Object createJavaScriptObject(JsValue value,
      CompilingClassLoader classLoader) {
    Throwable caught;
    try {
      // See if there's already a wrapper object (assures identity comparison).
      Object jso = classLoader.getCachedJso(value.getJavaScriptObjectPointer());
      if (jso != null) {
        return jso;
      }

      // Instantiate the JSO class.
      Class<?> jsoType = Class.forName(JSO_IMPL_CLASS, true, classLoader);
      Constructor<?> ctor = jsoType.getDeclaredConstructor();
      ctor.setAccessible(true);
      jso = ctor.newInstance();

      // Set the reference field to this JsValue using reflection.
      Field referenceField = jsoType.getField(HOSTED_MODE_REFERENCE);
      referenceField.set(jso, value);

      classLoader.putCachedJso(value.getJavaScriptObjectPointer(), jso);
      return jso;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e;
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (NoSuchFieldException e) {
      caught = e;
    }
    throw new RuntimeException("Error creating JavaScript object", caught);
  }

  /**
   * Return an object containing the value JavaScript object as a specified
   * type.
   *
   * @param value the JavaScript value
   * @param type expected type of the returned object
   * @param msgPrefix a prefix for error/warning messages
   * @return the object reference
   * @throws com.google.gwt.dev.shell.HostedModeException if the JavaScript
   *     object is not assignable to the supplied type.
   */
  @SuppressWarnings("unchecked")
  public static <T> T get(JsValue value, CompilingClassLoader cl,
      Class<T> type, String msgPrefix) {

    if (type.isPrimitive()) {
      if (type == Boolean.TYPE) {
        if (!value.isBoolean()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected boolean");
        }
        return (T) Boolean.valueOf(value.getBoolean());
      } else if (type == Byte.TYPE) {
        return (T) Byte.valueOf((byte) getIntRange(value, Byte.MIN_VALUE,
            Byte.MAX_VALUE, "byte", msgPrefix));
      } else if (type == Character.TYPE) {
        return (T) Character.valueOf((char) getIntRange(value,
            Character.MIN_VALUE, Character.MAX_VALUE, "char", msgPrefix));
      } else if (type == Double.TYPE) {
        if (!value.isNumber()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected double");
        }
        return (T) Double.valueOf(value.getNumber());
      } else if (type == Float.TYPE) {
        if (!value.isNumber()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected float");
        }
        double doubleVal = value.getNumber();

        // Check for small changes near MIN_VALUE and replace with the
        // actual end point value, in case it is being used as a sentinel
        // value. This test works by the subtraction result rounding off to
        // zero if the delta is not representable in a float.
        // TODO(jat): add similar test for MAX_VALUE if we have a JS
        // platform that alters the value while converting to/from strings.
        if ((float) (doubleVal - Float.MIN_VALUE) == 0.0f) {
          doubleVal = Float.MIN_VALUE;
        }

        float floatVal = (float) doubleVal;
        if (Float.isInfinite(floatVal) && !Double.isInfinite(doubleVal)) {
          // in this case we had overflow from the double value which was
          // outside the range of supported float values, and the cast
          // converted it to infinity. Since this lost data, we treat this
          // as an error in hosted mode.
          throw new HostedModeException(msgPrefix + ": JS value " + doubleVal
              + " out of range for a float");
        }
        return (T) Float.valueOf(floatVal);
      } else if (type == Integer.TYPE) {
        return (T) Integer.valueOf(getIntRange(value, Integer.MIN_VALUE,
            Integer.MAX_VALUE, "int", msgPrefix));
      } else if (type == Long.TYPE) {
        if (!value.isWrappedJavaObject()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected Java long");
        }
        JavaLong javaLong = (JavaLong) value.getWrappedJavaObject();
        return (T) Long.valueOf(javaLong.longValue());
      } else if (type == Short.TYPE) {
        return (T) Short.valueOf((short) getIntRange(value, Short.MIN_VALUE,
            Short.MAX_VALUE, "short", msgPrefix));
      }
    }

    if (value.isNull() || value.isUndefined()) {
      return null;
    }
    if (value.isWrappedJavaObject()) {
      return type.cast(value.getWrappedJavaObject());
    }
    if (value.isString()) {
      return type.cast(value.getString());
    }
    if (value.isJavaScriptObject()) {
      return type.cast(createJavaScriptObject(value, cl));
    }

    // Just don't know what do to with this.
    /*
     * TODO (amitmanjhi): does throwing a HostedModeException here and catching
     * a RuntimeException in user test
     * com.google.gwt.dev.jjs.test.HostedTest::testObjectReturns() make sense
     */
    throw new IllegalArgumentException(msgPrefix + ": JS value of type "
        + value.getTypeString() + ", expected "
        + TypeInfo.getSourceRepresentation(type));
  }

  /**
   * Set the underlying value.
   *
   * @param value JsValue to set
   * @param type static type of the object
   * @param obj the object to store in the JS value
   */
  public static void set(JsValue value, CompilingClassLoader cl, Class<?> type,
      Object obj) {
    if (type.isPrimitive()) {
      if (type == Boolean.TYPE) {
        value.setBoolean(((Boolean) obj).booleanValue());
      } else if (type == Byte.TYPE) {
        value.setInt(((Byte) obj).byteValue());
      } else if (type == Character.TYPE) {
        value.setInt(((Character) obj).charValue());
      } else if (type == Double.TYPE) {
        value.setDouble(((Double) obj).doubleValue());
      } else if (type == Float.TYPE) {
        value.setDouble(((Float) obj).floatValue());
      } else if (type == Integer.TYPE) {
        value.setInt(((Integer) obj).intValue());
      } else if (type == Long.TYPE) {
        long longVal = ((Long) obj).longValue();
        value.setWrappedJavaObject(cl, new JavaLong(longVal));
      } else if (type == Short.TYPE) {
        value.setInt(((Short) obj).shortValue());
      } else if (type == Void.TYPE) {
        value.setUndefined();
      } else {
        throw new HostedModeException("Cannot marshal primitive type " + type);
      }
    } else if (obj == null) {
      value.setNull();
    } else {
      // not a boxed primitive
      try {
        Class<?> jsoType = Class.forName(JSO_IMPL_CLASS, false, cl);
        if (jsoType == obj.getClass()) {
          JsValue jsObject = getUnderlyingObject(obj);
          value.setValue(jsObject);
          return;
        }
      } catch (ClassNotFoundException e) {
        // Ignore the exception, if we can't find the class then obviously we
        // don't have to worry about o being one
      }

      // Fall through case: Object.
      if (!type.isInstance(obj)) {
        throw new HostedModeException("object is of type "
            + obj.getClass().getName() + ", expected " + type.getName());
      }
      if (obj instanceof String) {
        value.setString((String) obj);
      } else {
        value.setWrappedJavaObject(cl, obj);
      }
    }
  }

  private static int getIntRange(JsValue value, int low, int high,
      String typeName, String msgPrefix) {
    int intVal;
    if (value.isInt()) {
      intVal = value.getInt();
      if (intVal < low || intVal > high) {
        throw new HostedModeException(msgPrefix + ": JS int value " + intVal
            + " out of range for a " + typeName);
      }
    } else if (value.isNumber()) {
      double doubleVal = value.getNumber();
      if (doubleVal < low || doubleVal > high) {
        throw new HostedModeException(msgPrefix + ": JS double value "
            + doubleVal + " out of range for a " + typeName);
      }
      intVal = (int) doubleVal;
      if (intVal != doubleVal) {
        ModuleSpace.getLogger().log(TreeLogger.WARN,
            msgPrefix + ": Rounding double (" + doubleVal + ") to int for "
            + typeName, null);
      }
    } else {
      throw new HostedModeException(msgPrefix + ": JS value of type "
          + value.getTypeString() + ", expected " + typeName);
    }
    return intVal;
  }

  /**
   * Returns the underlying JsValue from a JavaScriptObject instance.
   *
   * The tricky part is that it is in a different ClassLoader so therefore can't
   * be specified directly. The type is specified as Object, and reflection is
   * used to retrieve the reference field.
   *
   * @param jso the instance of JavaScriptObject to retrieve the JsValue from.
   * @return the JsValue representing the JavaScript object
   */
  private static JsValue getUnderlyingObject(Object jso) {
    Throwable caught;
    try {
      Field referenceField = jso.getClass().getField(HOSTED_MODE_REFERENCE);
      referenceField.setAccessible(true);
      return (JsValue) referenceField.get(jso);
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchFieldException e) {
      caught = e;
    }
    throw new RuntimeException("Error reading " + HOSTED_MODE_REFERENCE, caught);
  }

  private JsValueGlue() {
  }
}
