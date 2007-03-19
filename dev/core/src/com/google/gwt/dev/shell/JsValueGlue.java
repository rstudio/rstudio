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
import com.google.gwt.dev.util.TypeInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Glue layer that performs GWT-specific operations on JsValues. Used to isolate
 * HostedModeExceptions/etc from JsValue code
 */
public class JsValueGlue {
  public static final String JSO_CLASS = "com.google.gwt.core.client.JavaScriptObject";

  /**
   * Create a JavaScriptObject instance referring to this JavaScript object.
   * 
   * The caller is responsible for ensuring that the requested type is a
   * subclass of JavaScriptObject.
   * 
   * @param type The subclass of JavaScriptObject to create
   * @return the constructed JavaScriptObject
   */
  public static Object createJavaScriptObject(JsValue value, Class type) {
    try {
      // checkThread();
      if (!value.isJavaScriptObject()) {
        throw new RuntimeException(
            "Only Object type JavaScript objects can be made into JavaScriptObject");
      }

      /* find the JavaScriptObject type, while verifying this is a subclass */
      Class jsoType = getJavaScriptObjectSuperclass(type);
      if (jsoType == null) {
        throw new RuntimeException("Requested type " + type.getName()
            + " not a subclass of JavaScriptObject");
      }

      /* create the object using the default constructor */
      Constructor ctor = type.getDeclaredConstructor(new Class[] {});
      ctor.setAccessible(true);
      Object jso = ctor.newInstance(new Object[] {});

      /* set the hostedModeReference field to this JsValue using reflection */
      Field referenceField = jsoType.getDeclaredField("hostedModeReference");
      referenceField.setAccessible(true);
      referenceField.set(jso, value);
      return jso;
    } catch (InstantiationException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (SecurityException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Error creating JavaScript object", e);
    }
  }

  /**
   * Return an object containing the value JavaScript object as a specified
   * type.
   * 
   * @param value the JavaScript value
   * @param type expected type of the returned object
   * @param msgPrefix a prefix for error/warning messages
   * @return the object reference
   * @throws HostedModeException if the JavaScript object is not assignable to
   *           the supplied type.
   */
  public static Object get(JsValue value, Class type, String msgPrefix) {
    double doubleVal;
    if (value.isNull()) {
      return null;
    }
    if (value.isUndefined()) {
      // undefined is never legal to return from JavaScript into Java
      throw new HostedModeException(msgPrefix
          + ": JavaScript undefined, expected " + type.getName());
   }
    if (value.isWrappedJavaObject()) {
      Object origObject = value.getWrappedJavaObject();
      if (!type.isAssignableFrom(origObject.getClass())) {
        throw new HostedModeException(msgPrefix + ": Java object of type "
            + origObject.getClass().getName() + ", expected " + type.getName());
      }
      return origObject;
    }
    if (getJavaScriptObjectSuperclass(type) != null) {
      if (!value.isJavaScriptObject()) {
        throw new HostedModeException(msgPrefix + ": JS object of type "
            + value.getTypeString() + ", expected " + type.getName());
      }
      return createJavaScriptObject(value, type);
    }
    switch (TypeInfo.classifyType(type)) {
      case TypeInfo.TYPE_WRAP_BOOLEAN:
      case TypeInfo.TYPE_PRIM_BOOLEAN:
        if (!value.isBoolean()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected boolean");
        }
        return Boolean.valueOf(value.getBoolean());

      case TypeInfo.TYPE_WRAP_BYTE:
      case TypeInfo.TYPE_PRIM_BYTE:
        return new Byte((byte) getIntRange(value, Byte.MIN_VALUE,
            Byte.MAX_VALUE, "byte", msgPrefix));

      case TypeInfo.TYPE_WRAP_CHAR:
      case TypeInfo.TYPE_PRIM_CHAR:
        return new Character((char) getIntRange(value, Character.MIN_VALUE,
            Character.MAX_VALUE, "char", msgPrefix));

      case TypeInfo.TYPE_WRAP_DOUBLE:
      case TypeInfo.TYPE_PRIM_DOUBLE:
        if (!value.isNumber()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected double");
        }
        return new Double(value.getNumber());

      case TypeInfo.TYPE_WRAP_FLOAT:
      case TypeInfo.TYPE_PRIM_FLOAT:
        if (!value.isNumber()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected float");
        }
        doubleVal = value.getNumber();
        
        // Check for small changes near MIN_VALUE and replace with the
        // actual endpoint value, in case it is being used as a sentinel
        // value.  This test works by the subtraction result rounding off to
        // zero if the delta is not representable in a float.
        // TODO(jat): add similar test for MAX_VALUE if we have a JS
        // platform that munges the value while converting to/from strings.
        if ((float)(doubleVal - Float.MIN_VALUE) == 0.0f) {
          doubleVal = Float.MIN_VALUE;
        }
        
        float floatVal = (float)doubleVal;
        if (Float.isInfinite(floatVal) && !Double.isInfinite(doubleVal)) {
          // in this case we had overflow from the double value which was
          // outside the range of supported float values, and the cast
          // converted it to infinity.  Since this lost data, we treat this
          // as an error in hosted mode.
          throw new HostedModeException(msgPrefix + ": JS value " + doubleVal
              + " out of range for a float");
        }
        return new Float(floatVal);

      case TypeInfo.TYPE_WRAP_INT:
      case TypeInfo.TYPE_PRIM_INT:
        return new Integer(getIntRange(value, Integer.MIN_VALUE,
            Integer.MAX_VALUE, "int", msgPrefix));

      case TypeInfo.TYPE_WRAP_LONG:
      case TypeInfo.TYPE_PRIM_LONG:
        if (!value.isNumber()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected long");
        }
        doubleVal = value.getNumber();
        if (doubleVal < Long.MIN_VALUE || doubleVal > Long.MAX_VALUE) {
          throw new HostedModeException(msgPrefix + ": JS double value " + doubleVal
              + " out of range for a long");
        }
        // TODO(jat): can this actually detect loss of precision?
        long longVal = (long) doubleVal;
        if (doubleVal != longVal) {
          // TODO(jat): should this be an error or exception?
          ModuleSpace.getLogger().log(TreeLogger.WARN, msgPrefix
              + ": Loss of precision converting double to long", null);
        }
        return new Long(longVal);

      case TypeInfo.TYPE_WRAP_SHORT:
      case TypeInfo.TYPE_PRIM_SHORT:
        return new Short((short) getIntRange(value, Short.MIN_VALUE,
            Short.MAX_VALUE, "short", msgPrefix));

      case TypeInfo.TYPE_WRAP_STRING:
        if (!value.isString()) {
          throw new HostedModeException(msgPrefix + ": JS value of type "
              + value.getTypeString() + ", expected string");
        }
        return value.getString();

      case TypeInfo.TYPE_USER:
        if (value.isString()) {
          return value.getString();
        }
        // if it isn't a String, it's an error, break to error
        break;
    }

    // Just don't know what do to with this.
    throw new IllegalArgumentException(msgPrefix + ": Cannot convert to type "
        + TypeInfo.getSourceRepresentation(type, "") + " from "
        + value.getTypeString());
  }

  /**
   * Returns the underlying JsValue from a JavaScriptObject instance.
   * 
   * The tricky part is that it is in a different classloader so therefore can't
   * be specified directly. The type is specified as Object, and reflection is
   * used to retrieve the hostedModeReference field.
   * 
   * @param jso the instance of JavaScriptObject to retrieve the JsValue from.
   * @return the JsValue representing the JavaScript object
   */
  public static JsValue getUnderlyingObject(Object jso) {
    try {
      /*
       * verify that jso is assignable to
       * com.google.gwt.core.client.JavaScriptObject
       */
      Class type = getJavaScriptObjectSuperclass(jso.getClass());

      if (type == null) {
        throw new HostedModeException(
            "Underlying JSO not a subclass of JavaScriptObject");
      }

      Field referenceField = type.getDeclaredField("hostedModeReference");
      referenceField.setAccessible(true);
      return (JsValue) referenceField.get(jso);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Error reading handle", e);
    } catch (SecurityException e) {
      throw new RuntimeException("Error reading handle", e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Error reading handle", e);
    }
  }

  /**
   * Set the underlying value.
   * 
   * @param value JsValue to set
   * @param type static type of the object
   * @param obj the object to store in the JS value
   */
  public static void set(JsValue value, CompilingClassLoader cl, Class type,
      Object obj) {
    if (obj == null) {
      value.setNull();
    } else if (type.equals(String.class)) {
      value.setString((String) obj);
    } else if (type.equals(boolean.class)) {
      value.setBoolean(((Boolean) obj).booleanValue());
    } else if (type.equals(short.class)) {
      value.setInt(((Short) obj).shortValue());
    } else if (type.equals(int.class)) {
      value.setInt(((Integer) obj).intValue());
    } else if (type.equals(byte.class)) {
      value.setInt(((Byte) obj).byteValue());
    } else if (type.equals(char.class)) {
      value.setInt(((Character) obj).charValue());
    } else if (type.equals(long.class)) {
      long longVal = ((Long) obj).longValue();
      double doubleVal = longVal;
      if ((long) doubleVal != longVal) {
        // TODO(jat): should this be an error or exception?
        ModuleSpace.getLogger().log(TreeLogger.WARN,
            "Loss of precision converting long to double", null);
      }
      value.setDouble(doubleVal);
    } else if (type.equals(float.class)) {
      value.setDouble(((Float) obj).floatValue());
    } else if (type.equals(double.class)) {
      value.setDouble(((Double) obj).doubleValue());
    } else {
      // not a boxed primitive
      try {
        Class jso = Class.forName(JSO_CLASS, true, cl);
        if (jso.isAssignableFrom(type) && jso.isAssignableFrom(obj.getClass())) {
          JsValue jsObject = getUnderlyingObject(obj);
          value.setValue(jsObject);
          return;
        }
      } catch (ClassNotFoundException e) {
        // Ignore the exception, if we can't find the class then obviously we
        // don't have to worry about o being one
      }

      // Fallthrough case: Object.
      if (!type.isAssignableFrom(obj.getClass())) {
          throw new HostedModeException("object is of type "
              + obj.getClass().getName() + ", expected " + type.getName());
        }
      value.setWrappedJavaObject(cl, obj);
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
        ModuleSpace.getLogger().log(TreeLogger.WARN, msgPrefix
            + ": Rounding double to int for " + typeName, null);
      }
    } else {
      throw new HostedModeException(msgPrefix + ": JS value of type "
          + value.getTypeString() + ", expected " + typeName);
    }
    return intVal;
  }

  /**
   * Verify that the supplied class is a subclass of
   * com.google.gwt.core.client.JavaScriptObject, and return the
   * JavaScriptObject class if it is. This is required since JavaScriptObject
   * actually lives in a different classloader and can't be referenced directly.
   * 
   * @param type class to test
   * @return the JavaScriptObject class object if it is a subclass, or null if
   *         not.
   */
  private static Class getJavaScriptObjectSuperclass(Class type) {
    while (type != null && !type.getName().equals(JSO_CLASS)) {
      type = type.getSuperclass();
    }
    return type;
  }
}
