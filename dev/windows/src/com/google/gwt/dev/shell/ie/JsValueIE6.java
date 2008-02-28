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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.LowLevel;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.DISPPARAMS;
import org.eclipse.swt.internal.ole.win32.EXCEPINFO;
import org.eclipse.swt.internal.ole.win32.GUID;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.internal.ole.win32.IUnknown;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.ole.win32.Variant;

/**
 * Represents an IE JavaScript value.
 */
public class JsValueIE6 extends JsValue {

  private static class JsCleanupIE6 implements JsCleanup {
    private Variant variant;

    public JsCleanupIE6(Variant variant) {
      this.variant = variant;
    }

    public void doCleanup() {
      if (variant != null) {
        variant.dispose();
      }
    }
  }

  private static Variant maybeCopyVariant(Variant variant) {
    if (variant == null) {
      return new Variant();
    }
    switch (variant.getType()) {
      case COM.VT_DISPATCH: {
        IDispatch dispatch = variant.getDispatch();
        dispatch.AddRef();
        return new Variant(dispatch);
      }
      case COM.VT_UNKNOWN: {
        IUnknown unknown = variant.getUnknown();
        unknown.AddRef();
        return new Variant(unknown);
      }
    }
    return variant;
  }

  /**
   * Copied with modification from OleAutomation.getIDsOfNames(). The reason we
   * don't use that method directly is that querying for typeInfo that occurs in
   * the OleAutomation(IDispatch) constructor will cause a VM crash on some
   * kinds of JavaScript objects, such as the window.alert function. So we do it
   * by hand.
   */
  private static int[] oleAutomationGetIdsOfNames(IDispatch dispatch) {
    String[] names = new String[] {"valueOf"};
    int[] ids = new int[names.length];
    int result = dispatch.GetIDsOfNames(new GUID(), names, names.length,
        COM.LOCALE_USER_DEFAULT, ids);
    if (result != COM.S_OK) {
      return null;
    }
    return ids;
  }

  /**
   * Copied with modification from OleAutomation.invoke(). The reason we don't
   * use that method directly is that querying for typeInfo that occurs in the
   * OleAutomation(IDispatch) constructor will cause a VM crash on some kinds of
   * JavaScript objects, such as the window.alert function. So we do it by hand.
   */
  private static Variant oleAutomationInvoke(IDispatch dispatch, int dispId) {
    int pVarResultAddress = 0;
    try {
      pVarResultAddress = OS.GlobalAlloc(OS.GMEM_FIXED | OS.GMEM_ZEROINIT,
          Variant.sizeof);
      int[] pArgErr = new int[1];
      int hr = dispatch.Invoke(dispId, new GUID(), COM.LOCALE_USER_DEFAULT,
          COM.DISPATCH_METHOD, new DISPPARAMS(), pVarResultAddress,
          new EXCEPINFO(), pArgErr);

      if (hr >= COM.S_OK) {
        return Variant.win32_new(pVarResultAddress);
      }
    } finally {
      if (pVarResultAddress != 0) {
        COM.VariantClear(pVarResultAddress);
        OS.GlobalFree(pVarResultAddress);
      }
    }
    return null;
  }

  // a null variant means the JsValue is undefined (void)
  private Variant variant;

  /**
   * Create a null JsValue.
   */
  public JsValueIE6() {
    this.variant = null;
  }

  /**
   * Create a JsValue given a variant.
   * 
   * @param variant JS value
   */
  public JsValueIE6(Variant variant) {
    this.variant = maybeCopyVariant(variant);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getBoolean()
   */
  @Override
  public boolean getBoolean() {
    return variant.getBoolean();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getInt()
   */
  @Override
  public int getInt() {
    return variant.getInt();
  }

  @Override
  public int getJavaScriptObjectPointer() {
    if (isJavaScriptObject()) {
      return variant.getDispatch().getAddress();
    } else {
      return 0;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getNumber()
   */
  @Override
  public double getNumber() {
    return variant.getDouble();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getString()
   */
  @Override
  public String getString() {
    return variant.getString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getTypeString()
   */
  @Override
  public String getTypeString() {
    switch (variant.getType()) {
      case COM.VT_BOOL:
        return "boolean";
      case COM.VT_I1:
      case COM.VT_I2:
      case COM.VT_I4:
      case COM.VT_I8:
      case COM.VT_UI1:
      case COM.VT_UI2:
      case COM.VT_UI4:
      case COM.VT_R4:
      case COM.VT_R8:
        return "number";
      case COM.VT_BSTR:
        return "string";
      case COM.VT_EMPTY:
        return "undefined";
      case COM.VT_NULL:
        return "null";
      case COM.VT_DISPATCH:
        return isWrappedJavaObject() ? "Java Object" : "JavaScript object";
      default:
        return "unexpected Variant type";
    }
  }

  /**
   * Return the underlying Variant object. PLATFORM-SPECIFIC.
   */
  public Variant getVariant() {
    return maybeCopyVariant(variant);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getWrappedJavaObject()
   */
  @Override
  public Object getWrappedJavaObject() {
    return tryToUnwrapWrappedJavaObject();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isBoolean()
   */
  @Override
  public boolean isBoolean() {
    return variant != null && variant.getType() == COM.VT_BOOL;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isInt()
   */
  @Override
  public boolean isInt() {
    if (variant == null) {
      return false;
    }
    switch (variant.getType()) {
      case COM.VT_I1:
      case COM.VT_I2:
      case COM.VT_I4:
      case COM.VT_UI1:
      case COM.VT_UI2:
        // note that VT_UI4 is excluded since it may not fit in an int
        return true;
      default:
        return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isJavaScriptObject()
   */
  @Override
  public boolean isJavaScriptObject() {
    if (variant == null) {
      return false;
    }
    if (variant.getType() != COM.VT_DISPATCH) {
      return false;
    }
    return tryToUnwrapWrappedJavaObject() == null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isNull()
   */
  @Override
  public boolean isNull() {
    return variant != null && variant.getType() == COM.VT_NULL;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isNumber()
   */
  @Override
  public boolean isNumber() {
    if (variant == null) {
      return false;
    }
    switch (variant.getType()) {
      case COM.VT_I1:
      case COM.VT_I2:
      case COM.VT_I4:
      case COM.VT_I8:
      case COM.VT_UI1:
      case COM.VT_UI2:
      case COM.VT_UI4:
      case COM.VT_R4:
      case COM.VT_R8:
        return true;
      default:
        return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isString()
   */
  @Override
  public boolean isString() {
    if (variant == null) {
      return false;
    }
    if (variant.getType() == COM.VT_BSTR) {
      return true;
    }
    // see if the variant is a wrapper object
    if (variant.getType() != COM.VT_DISPATCH) {
      return false;
    }

    // see if it has a valueOf method
    IDispatch dispatch = variant.getDispatch();
    int[] ids = oleAutomationGetIdsOfNames(dispatch);
    if (ids == null) {
      return false;
    }
    Variant result = null;
    try {
      result = oleAutomationInvoke(dispatch, ids[0]);
      /*
       * If the return type of the valueOf method is string, we assume it is a
       * String wrapper object.
       */
      return result.getType() == COM.VT_BSTR;
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isUndefined()
   */
  @Override
  public boolean isUndefined() {
    return variant == null || variant.getType() == COM.VT_EMPTY;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isWrappedJavaObject()
   */
  @Override
  public boolean isWrappedJavaObject() {
    if (variant == null) {
      return false;
    }
    if (variant.getType() != COM.VT_DISPATCH) {
      return false;
    }
    return tryToUnwrapWrappedJavaObject() != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setBoolean(boolean)
   */
  @Override
  public void setBoolean(boolean val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setByte(byte)
   */
  @Override
  public void setByte(byte val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setChar(char)
   */
  @Override
  public void setChar(char val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setDouble(double)
   */
  @Override
  public void setDouble(double val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setInt(int)
   */
  @Override
  public void setInt(int val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setNull()
   */
  @Override
  public void setNull() {
    setVariant(new Variant(0, COM.VT_NULL));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setShort(short)
   */
  @Override
  public void setShort(short val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setString(java.lang.String)
   */
  @Override
  public void setString(String val) {
    setVariant(new Variant(val));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setUndefined()
   */
  @Override
  public void setUndefined() {
    setVariant(null);
  }

  @Override
  public void setValue(JsValue other) {
    setVariant(maybeCopyVariant(((JsValueIE6) other).variant));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setWrappedJavaObject(com.google.gwt.dev.shell.CompilingClassLoader,
   *      java.lang.Object)
   */
  @Override
  public <T> void setWrappedJavaObject(CompilingClassLoader cl, T val) {
    IDispatchImpl dispObj;
    if (val == null) {
      setNull();
      return;
    } else if (val instanceof IDispatchImpl) {
      dispObj = (IDispatchImpl) val;
    } else {
      dispObj = new IDispatchProxy(cl, val);
    }
    IDispatch disp = new IDispatch(dispObj.getAddress());
    disp.AddRef();
    setVariant(new Variant(disp));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#createCleanupObject()
   */
  @Override
  protected JsCleanup createCleanupObject() {
    return new JsCleanupIE6(variant);
  }

  /**
   * Reset the underlying variant, freeing the old one if necessary.
   * 
   * @param val the new Variant to store
   */
  protected void setVariant(Variant val) {
    if (variant != null) {
      variant.dispose();
    }
    variant = val;
  }

  private Object tryToUnwrapWrappedJavaObject() {
    int globalRef = 0;
    Variant result = null;
    try {
      result = oleAutomationInvoke(variant.getDispatch(),
          IDispatchProxy.DISPID_MAGIC_GETGLOBALREF);
      if (result != null) {
        globalRef = result.getInt();
        if (globalRef != 0) {
          // This is really a Java object being passed back via an
          // IDispatchProxy.
          IDispatchProxy proxy = (IDispatchProxy) LowLevel.objFromGlobalRefInt(globalRef);
          return proxy.getTarget();
        }
      }
      return null;
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

}
