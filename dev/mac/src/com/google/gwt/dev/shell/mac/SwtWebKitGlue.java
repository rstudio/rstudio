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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;
import com.google.gwt.dev.util.TypeInfo;

/**
 * A bag of static helper methods for mucking about with low-level SWT and Gecko
 * constructs.
 */
class SwtWebKitGlue {

  /**
   * Try to convert based on the Java method parameter type.
   */
  public static Object convertJSValToObject(Class paramType,
      int jsval) {
    if (LowLevelSaf.isNull(jsval) || LowLevelSaf.isUndefined(jsval)) {
      // It is actually a null reference.
      return null;
    }

    if (LowLevelSaf.isObject(jsval)) {
      Object translated = translateJSObject(paramType, jsval);

      // Make sure that the method we are going to call matches on this
      // parameter.
      if (paramType.isAssignableFrom(translated.getClass())) {
        return translated;
      }
    }

    int curExecState = LowLevelSaf.getExecState();

    switch (TypeInfo.classifyType(paramType)) {
      case TypeInfo.TYPE_WRAP_BOOLEAN:
      case TypeInfo.TYPE_PRIM_BOOLEAN:
        return Boolean.valueOf(LowLevelSaf.coerceToBoolean(curExecState, jsval));
      case TypeInfo.TYPE_WRAP_BYTE:
      case TypeInfo.TYPE_PRIM_BYTE:
        return new Byte(LowLevelSaf.coerceToByte(curExecState, jsval));
      case TypeInfo.TYPE_WRAP_CHAR:
      case TypeInfo.TYPE_PRIM_CHAR:
        return new Character(LowLevelSaf.coerceToChar(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_DOUBLE:
      case TypeInfo.TYPE_PRIM_DOUBLE:
        return new Double(LowLevelSaf.coerceToDouble(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_FLOAT:
      case TypeInfo.TYPE_PRIM_FLOAT:
        return new Float(LowLevelSaf.coerceToFloat(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_INT:
      case TypeInfo.TYPE_PRIM_INT:
        return new Integer(LowLevelSaf.coerceToInt(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_LONG:
      case TypeInfo.TYPE_PRIM_LONG:
        return new Long(LowLevelSaf.coerceToLong(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_SHORT:
      case TypeInfo.TYPE_PRIM_SHORT:
        return new Short(LowLevelSaf.coerceToShort(curExecState, jsval));

      case TypeInfo.TYPE_WRAP_STRING:
        return LowLevelSaf.coerceToString(curExecState, jsval);

      case TypeInfo.TYPE_USER:
        if (LowLevelSaf.isString(jsval)) {
          return LowLevelSaf.coerceToString(curExecState, jsval);
        }
        // if it isn't a String, it's an error, break to error
        break;
    }

    // Just don't know what do to with this.
    throw new IllegalArgumentException("Cannot convert to type "
        + TypeInfo.getSourceRepresentation(paramType, ""));
  }

  /**
   * Converts a java object to its equivalent variant. A ClassLoader is passed
   * here so that Handles can be manipulated properly.
   */
  public static int convertObjectToJSVal(int scriptObject,
      CompilingClassLoader cl, Class type, Object o) {
    if (o == null) {
      return LowLevelSaf.jsNull();
    }

    if (type.equals(String.class)) {
      return LowLevelSaf.convertString((String) o);
    } else if (type.equals(boolean.class)) {
      return LowLevelSaf.convertBoolean(((Boolean) o).booleanValue());
    } else if (type.equals(byte.class)) {
      return LowLevelSaf.convertDouble(((Byte) o).byteValue());
    } else if (type.equals(short.class)) {
      return LowLevelSaf.convertDouble(((Short) o).shortValue());
    } else if (type.equals(char.class)) {
      return LowLevelSaf.convertDouble(((Character) o).charValue());
    } else if (type.equals(int.class)) {
      return LowLevelSaf.convertDouble(((Integer) o).intValue());
    } else if (type.equals(long.class)) {
      return LowLevelSaf.convertDouble(((Long) o).longValue());
    } else if (type.equals(float.class)) {
      return LowLevelSaf.convertDouble(((Float) o).floatValue());
    } else if (type.equals(double.class)) {
      return LowLevelSaf.convertDouble(((Double) o).doubleValue());
    }

    // Handle
    try {
      Class jso = Class.forName(HandleSaf.HANDLE_CLASS, true, cl);
      if (jso.isAssignableFrom(type) && jso.isAssignableFrom(o.getClass())) {
        // Variant never AddRef's its contents.
        //
        return HandleSaf.getJSObjectFromHandle(o);
      }
    } catch (ClassNotFoundException e) {
      // Ignore the exception, if we can't find the class then obviously we
      // don't have to worry about o being one
    }

    // Fallthrough case: Object.
    //
    return wrapObjectAsJSObject(cl, scriptObject, o);
  }

  /**
   * Wraps a Java object as a JSObject.
   */
  public static int wrapObjectAsJSObject(CompilingClassLoader cl,
      int scriptObject, Object jthis) {
    if (jthis == null) {
      return LowLevelSaf.jsNull();
    }

    DispatchObject dispObj;
    if (jthis instanceof DispatchObject) {
      dispObj = (DispatchObject) jthis;
    } else {
      dispObj = new WebKitDispatchAdapter(cl, scriptObject, jthis);
    }
    return LowLevelSaf.wrapDispatch(dispObj);
  }

  /**
   * Decides what to do with an incoming JSObject arg. Two possibilities here:
   * (1) We received a true javascript object (e.g. DOM object), in which case
   * we wrap it in a Handle. (2) We received a Java object that was passed
   * through the outside world and back, in which case we use black magic to get
   * it back.
   */
  private static Object translateJSObject(Class type, int jsval) {
    if (LowLevelSaf.isWrappedDispatch(jsval)) {
      DispatchObject dispObj = LowLevelSaf.unwrapDispatch(jsval);
      return dispObj.getTarget();
    }
    return HandleSaf.createHandle(type, jsval);
  }

}
