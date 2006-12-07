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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.COMObject;
import org.eclipse.swt.internal.ole.win32.DISPPARAMS;
import org.eclipse.swt.internal.ole.win32.GUID;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Basic IDispatch implementation for use by
 * {@link com.google.gwt.shell.ie.IDispatchProxy} and
 * {@link com.google.gwt.shell.ie.IDispatchStatic}.
 */
abstract class IDispatchImpl extends COMObject {

  /**
   * An exception for wrapping bad HR's.
   */
  protected static class HResultException extends Exception {
    private int fHR;

    private String fSource;

    /**
     * Constructs a standard bad HR exception.
     */
    public HResultException(int hr) {
      super(Integer.toString(hr));
      fHR = hr;
      fSource = "Java";
    }

    /**
     * Constructs a DISP_E_EXCEPTION bad HR.
     */
    public HResultException(String message) {
      super(message);
      fHR = COM.DISP_E_EXCEPTION;
      fSource = "Java";
    }

    /**
     * Constructs a DISP_E_EXCEPTION bad HR.
     */
    public HResultException(Throwable e) {
      super(AbstractTreeLogger.getStackTraceAsString(e), e);
      fHR = COM.DISP_E_EXCEPTION;
      fSource = "Java";
    }

    /**
     * If the HR is DISP_E_EXCEPTION, this method will fill in the EXCEPINFO
     * structure. Otherwise, it does nothing.
     */
    public void fillExcepInfo(int pExcepInfo) {
      if (fHR == COM.DISP_E_EXCEPTION) {
        SwtOleGlue.setEXCEPINFO(pExcepInfo, fHR, fSource, getMessage(), 0);
      }
    }

    /**
     * Gets the HR.
     */
    public int getHResult() {
      return fHR;
    }
  }

  // This one isn't defined in SWT for some reason.
  protected static final int DISP_E_UNKNOWNNAME = 0x80020006;

  protected static Variant callMethod(CompilingClassLoader cl, Object jthis,
      Variant[] params, Method method) throws InvocationTargetException,
      HResultException {
    Object[] javaParams = SwtOleGlue.convertVariantsToObjects(
        method.getParameterTypes(), params, "Calling method '"
            + method.getName() + "'");

    Object result = null;
    try {
      try {
        result = method.invoke(jthis, javaParams);
      } catch (IllegalAccessException e) {
        // should never, ever happen
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    } catch (NullPointerException e) {
      /*
       * The JavaScript expected the method to be static, having forgotten an
       * instance reference (most often "this.").
       */
      StringBuffer sb = new StringBuffer();
      sb.append("Instance method '");
      sb.append(method.getName());
      sb.append("' needed a qualifying instance ");
      sb.append("(did you forget to prefix the call with 'this.'?)");
      throw new HResultException(sb.toString());
    } finally {
      for (int i = 0; i < javaParams.length; i++) {
        if (javaParams[i] instanceof OleAutomation) {
          OleAutomation tmp = (OleAutomation) javaParams[i];
          tmp.dispose();
        }
      }
    }

    // Convert it to a variant (if the return type is void, return
    // a VT_EMPTY variant -- 'undefined' in JavaScript).
    //
    Class returnType = method.getReturnType();
    if (returnType.equals(Void.TYPE)) {
      return new Variant();
    }
    return SwtOleGlue.convertObjectToVariant(cl, returnType, result);
  }

  protected int refCount;

  public IDispatchImpl() {
    super(new int[] {2, 0, 0, 1, 3, 5, 8});
  }

  // CHECKSTYLE_OFF
  public int AddRef() {
    return ++refCount;
  }

  // CHECKSTYLE_ON

  public int method0(int[] args) {
    return QueryInterface(args[0], args[1]);
  }

  public int method1(int[] args) {
    return AddRef();
  }

  // method3 GetTypeInfoCount - not implemented

  // method4 GetTypeInfo - not implemented

  public int method2(int[] args) {
    return Release();
  }

  public int method5(int[] args) {
    return GetIDsOfNames(args[0], args[1], args[2], args[3], args[4]);
  }

  public int method6(int[] args) {
    return Invoke(args[0], args[1], args[2], args[3], args[4], args[5],
        args[6], args[7]);
  }

  // CHECKSTYLE_OFF
  public int QueryInterface(int riid, int ppvObject) {
    if (riid == 0 || ppvObject == 0) {
      return COM.E_NOINTERFACE;
    }
    GUID guid = new GUID();
    COM.MoveMemory(guid, riid, GUID.sizeof);

    if (COM.IsEqualGUID(guid, COM.IIDIUnknown)) {
      COM.MoveMemory(ppvObject, new int[] {getAddress()}, 4);
      AddRef();
      return COM.S_OK;
    }

    if (COM.IsEqualGUID(guid, COM.IIDIDispatch)) {
      COM.MoveMemory(ppvObject, new int[] {getAddress()}, 4);
      AddRef();
      return COM.S_OK;
    }

    COM.MoveMemory(ppvObject, new int[] {0}, 4);
    return COM.E_NOINTERFACE;
  }

  public int Release() {
    if (--refCount == 0) {
      dispose();
    }
    return refCount;
  }

  // CHECKSTYLE_ON

  /**
   * Override this method to implement GetIDsOfNames().
   */
  protected abstract void getIDsOfNames(String[] names, int[] ids)
      throws HResultException;

  /**
   * Override this method to implement Invoke().
   */
  protected abstract Variant invoke(int dispId, int flags, Variant[] params)
      throws HResultException, InvocationTargetException;

  private Variant[] extractVariantArrayFromDispParamsPtr(int pDispParams) {
    DISPPARAMS dispParams = new DISPPARAMS();
    COM.MoveMemory(dispParams, pDispParams, DISPPARAMS.sizeof);
    Variant[] variants = new Variant[dispParams.cArgs];
    // Reverse the order as we pull the variants in.
    for (int i = 0, n = dispParams.cArgs; i < n; ++i) {
      int varArgAddr = dispParams.rgvarg + Variant.sizeof * i;
      variants[n - i - 1] = Variant.win32_new(varArgAddr);
    }
    return variants;
  }

  // CHECKSTYLE_OFF
  private final int GetIDsOfNames(int riid, int rgszNames, int cNames,
      int lcid, int rgDispId) {

    try {
      if (cNames < 1) {
        return COM.E_INVALIDARG;
      }

      // Extract the requested names and build an answer array init'ed with -1.
      //
      String[] names = SwtOleGlue.extractStringArrayFromOleCharPtrPtr(
          rgszNames, cNames);
      int[] ids = new int[names.length];
      Arrays.fill(ids, -1);

      getIDsOfNames(names, ids);
      OS.MoveMemory(rgDispId, ids, ids.length * 4);
    } catch (HResultException e) {
      return e.getHResult();
    } catch (Throwable e) {
      e.printStackTrace();
      return COM.E_FAIL;
    }

    return COM.S_OK;
  }

  private int Invoke(int dispIdMember, int riid, int lcid, int dwFlags,
      int pDispParams, int pVarResult, int pExcepInfo, int pArgErr) {

    HResultException ex = null;
    Variant[] vArgs = null;
    Variant result = null;
    try {
      vArgs = extractVariantArrayFromDispParamsPtr(pDispParams);
      result = invoke(dispIdMember, dwFlags, vArgs);
      if (pVarResult != 0) {
        Variant.win32_copy(pVarResult, result);
      }
    } catch (HResultException e) {
      // Log to the console for detailed examination.
      //
      e.printStackTrace();
      ex = e;

    } catch (InvocationTargetException e) {
      // If we get here, it means an exception is being thrown from
      // Java back into JavaScript

      Throwable t = e.getTargetException();
      RuntimeException re;
      if (t instanceof RuntimeException) {
        re = (RuntimeException) t;
      } else {
        re = new RuntimeException("Checked exception thrown into JavaScript"
            + " (web mode behavior may differ)", t);
      }
      ex = new HResultException(re);
      ModuleSpace.setThrownJavaException(re);
    } catch (Exception e) {
      // Log to the console for detailed examination.
      //
      e.printStackTrace();
      ex = new HResultException(e);
    } finally {
      // We allocated variants for all arguments, so we must dispose them all.
      //
      for (int i = 0; i < vArgs.length; ++i) {
        if (vArgs[i] != null) {
          vArgs[i].dispose();
        }
      }

      if (result != null) {
        result.dispose();
      }
    }

    if (ex != null) {
      // Set up an exception for IE to throw.
      //
      ex.fillExcepInfo(pExcepInfo);
      return ex.getHResult();
    }

    return COM.S_OK;
  }
  // CHECKSTYLE_ON
}
