// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchObject;
import com.google.gwt.dev.util.TypeInfo;

import org.eclipse.swt.internal.mozilla.XPCOM;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A bag of static helper methods for mucking about with low-level SWT and Gecko
 * constructs.
 */
class SwtGeckoGlue {

  private static boolean areMethodsInitialized;
  private static final String ERRMSG_CANNOT_ACCESS = "Unable to find or access necessary SWT XPCOM methods.  Ensure that the correct version of swt.jar is being used.";
  private static final String ERRMSG_CANNOT_INVOKE = "Unable to invoke a necessary SWT XPCOM method.  Ensure that the correct version of swt.jar is being used.";
  private static Method XPCOMVtblCall;

  /**
   * Wrapper for XPCOM's nsISupports::AddRef().
   */
  public static int addRefInt(int nsISupports) {
    ensureMethodsInitialized();
    Throwable rethrow = null;
    try {
      Object retVal = XPCOMVtblCall.invoke(null, new Object[] {
          new Integer(1), new Integer(nsISupports)});
      return ((Integer) retVal).intValue();
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (InvocationTargetException e) {
      rethrow = e;
    }
    throw new RuntimeException(ERRMSG_CANNOT_INVOKE, rethrow);
  }

  /**
   * Wrapper for XPCOM's nsISupports::Release().
   */
  public static int releaseInt(int nsISupports) {
    ensureMethodsInitialized();
    Throwable rethrow = null;
    try {
      Object retVal = XPCOMVtblCall.invoke(null, new Object[] {
          new Integer(2), new Integer(nsISupports)});
      return ((Integer) retVal).intValue();
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (InvocationTargetException e) {
      rethrow = e;
    }
    throw new RuntimeException(ERRMSG_CANNOT_INVOKE, rethrow);
  }

  /**
   * Converts a java object to its equivalent variant. A ClassLoader is passed
   * here so that Handles can be manipulated properly.
   */
  public static int convertObjectToJSVal(int scriptObject,
      CompilingClassLoader cl, Class type, Object o) {
    if (o == null) {
      return LowLevelMoz.JSVAL_NULL;
    }

    if (type.equals(String.class)) {
      return LowLevelMoz.convertString(scriptObject, (String) o);
    } else if (type.equals(boolean.class)) {
      return LowLevelMoz.convertBoolean(scriptObject,
          ((Boolean) o).booleanValue());
    } else if (type.equals(byte.class)) {
      return LowLevelMoz.convertByte(scriptObject, ((Byte) o).byteValue());
    } else if (type.equals(short.class)) {
      return LowLevelMoz.convertShort(scriptObject, ((Short) o).shortValue());
    } else if (type.equals(char.class)) {
      return LowLevelMoz.convertChar(scriptObject, ((Character) o).charValue());
    } else if (type.equals(int.class)) {
      return LowLevelMoz.convertInt(scriptObject, ((Integer) o).intValue());
    } else if (type.equals(long.class)) {
      return LowLevelMoz.convertLong(scriptObject, ((Long) o).longValue());
    } else if (type.equals(float.class)) {
      return LowLevelMoz.convertFloat(scriptObject, ((Float) o).floatValue());
    } else if (type.equals(double.class)) {
      return LowLevelMoz.convertDouble(scriptObject, ((Double) o).doubleValue());
    }

    // Handle
    try {
      Class jso = Class.forName(HandleMoz.HANDLE_CLASS, true, cl);
      if (jso.isAssignableFrom(type) && jso.isAssignableFrom(o.getClass())) {
        // Variant never AddRef's its contents.
        //
        return HandleMoz.getJSObjectFromHandle(o);
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
      return LowLevelMoz.JSVAL_NULL;
    }

    DispatchObject dispObj;
    if (jthis instanceof DispatchObject) {
      dispObj = (DispatchObject) jthis;
    } else {
      dispObj = new GeckoDispatchAdapter(cl, scriptObject, jthis);
    }
    return LowLevelMoz.wrapDispatch(scriptObject, dispObj);
  }

  /**
   * Try to convert based on the Java method parameter type.
   */
  public static Object convertJSValToObject(int scriptObject, Class paramType,
      int jsval) {
    if (jsval == LowLevelMoz.JSVAL_VOID || jsval == LowLevelMoz.JSVAL_NULL) {
      // It is actually a null reference.
      return null;
    }

    if (LowLevelMoz.isJSObject(jsval)) {
      Object translated = translateJSObject(scriptObject, paramType, jsval);

      // Make sure that the method we are going to call matches on this
      // parameter.
      if (paramType.isAssignableFrom(translated.getClass())) {
        return translated;
      }
    }

    switch (TypeInfo.classifyType(paramType)) {
      case TypeInfo.TYPE_WRAP_BOOLEAN:
      case TypeInfo.TYPE_PRIM_BOOLEAN:
        return Boolean.valueOf(LowLevelMoz.coerceToBoolean(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_BYTE:
      case TypeInfo.TYPE_PRIM_BYTE:
        return new Byte(LowLevelMoz.coerceToByte(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_CHAR:
      case TypeInfo.TYPE_PRIM_CHAR:
        return new Character(LowLevelMoz.coerceToChar(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_DOUBLE:
      case TypeInfo.TYPE_PRIM_DOUBLE:
        return new Double(LowLevelMoz.coerceToDouble(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_FLOAT:
      case TypeInfo.TYPE_PRIM_FLOAT:
        return new Float(LowLevelMoz.coerceToFloat(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_INT:
      case TypeInfo.TYPE_PRIM_INT:
        return new Integer(LowLevelMoz.coerceToInt(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_LONG:
      case TypeInfo.TYPE_PRIM_LONG:
        return new Long(LowLevelMoz.coerceToLong(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_SHORT:
      case TypeInfo.TYPE_PRIM_SHORT:
        return new Short(LowLevelMoz.coerceToShort(scriptObject, jsval));

      case TypeInfo.TYPE_WRAP_STRING:
        return LowLevelMoz.coerceToString(scriptObject, jsval);

      case TypeInfo.TYPE_USER:
        if (LowLevelMoz.isString(jsval)) {
          return LowLevelMoz.coerceToString(scriptObject, jsval);
        }
        // if it isn't a String, it's an error, break to error
        break;
    }

    // Just don't know what do to with this.
    throw new IllegalArgumentException("Cannot convert to type "
        + TypeInfo.getSourceRepresentation(paramType, ""));
  }

  /**
   * Decides what to do with an incoming JSObject arg. Two possibilities here:
   * (1) We received a true javascript object (e.g. DOM object), in which case
   * we wrap it in a Handle. (2) We received a Java object that was passed
   * through the outside world and back, in which case we use black magic to get
   * it back.
   */
  private static Object translateJSObject(int scriptObject, Class type,
      int jsval) {
    if (LowLevelMoz.isWrappedDispatch(scriptObject, jsval)) {
      DispatchObject dispObj = LowLevelMoz.unwrapDispatch(scriptObject, jsval);
      return dispObj.getTarget();
    }
    int wrapper = 0;
    try {
      wrapper = LowLevelMoz.wrapJSObject(scriptObject, jsval);
      return HandleMoz.createHandle(type, wrapper);
    } finally {
      // Handle should AddRef
      if (wrapper != 0) {
        releaseInt(wrapper);
      }
    }
  }

  private static void ensureMethodsInitialized() {
    if (!areMethodsInitialized) {
      Throwable rethrow = null;

      // Never try more than once to initialize, even if there's an exception.
      areMethodsInitialized = true;
      try {
        XPCOMVtblCall = XPCOM.class.getDeclaredMethod("VtblCall", new Class[] {
            int.class, int.class});
        XPCOMVtblCall.setAccessible(true);
      } catch (SecurityException e) {
        rethrow = e;
      } catch (NoSuchMethodException e) {
        rethrow = e;
      }

      if (rethrow != null) {
        throw new RuntimeException(ERRMSG_CANNOT_ACCESS, rethrow);
      }
    }
  }

}
