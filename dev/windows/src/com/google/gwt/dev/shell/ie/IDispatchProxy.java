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
import com.google.gwt.dev.shell.JavaDispatch;
import com.google.gwt.dev.shell.JavaDispatchImpl;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.LowLevel;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.ole.win32.Variant;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary Java Object as an Automation-compatible server. The class
 * was motivated by the need to expose Java objects into JavaScript.
 * 
 * <p>
 * <b>Features</b>
 * </p>
 * <ul>
 * <li>Implements the <code>IDispatch</code> interface for you</li>
 * <li>If the COM client keeps a reference to this object, this object is
 * prevented from being garbage collected</li>
 * <li>Manages a JNI global ref on the target object so that it can be
 * reliabily passed back and forth between Java and external code </li>
 * <li>An instance of this class with no target is used to globally access all
 * static methods or fields.</li>
 * </ul>
 * 
 * <p>
 * <b>Limitations</b>
 * </p>
 * <ul>
 * <li>Only late-bound dispatch is supported</li>
 * <li>Named arguments are not supported (see {@link #GetIDsOfNames})).</li>
 * </ul>
 */
class IDispatchProxy extends IDispatchImpl {

  // A magic dispid for getting a global ref to this object.
  public static final int DISPID_MAGIC_GETGLOBALREF = 0xC131FB56;

  private final CompilingClassLoader classLoader;

  private boolean isDisposed = false;

  private final JavaDispatch javaDispatch;

  private final int myGlobalRef;

  /**
   * This constructor initializes as the static dispatcher, which handles only
   * static method calls and field references.
   * 
   * @param cl this class's classLoader
   */
  IDispatchProxy(CompilingClassLoader cl) {
    javaDispatch = new JavaDispatchImpl(cl);
    classLoader = cl;
    myGlobalRef = 0;
  }

  /**
   * This constructor initializes a dispatcher, around a particular instance.
   * 
   * @param cl this class's classLoader
   * @param target the object being wrapped as an IDispatch
   */
  IDispatchProxy(CompilingClassLoader cl, Object target) {
    javaDispatch = new JavaDispatchImpl(cl, target);
    classLoader = cl;
    myGlobalRef = LowLevel.newGlobalRefInt(this);
  }

  /**
   * Must be called when the object is no longer needed (to release the global
   * reference on the target object).
   */
  @Override
  public void dispose() {
    // Release the global ref on myself.
    if (myGlobalRef != 0) {
      LowLevel.deleteGlobalRefInt(myGlobalRef);
    }
    super.dispose();
    isDisposed = true;
  }

  public Object getTarget() {
    return javaDispatch.getTarget();
  }

  /**
   * Determine whether the proxy has already been disposed (this shouldn't be
   * necessary, but is needed by ModuleSpaceIE6 to workaround a bug in IE).
   */
  public boolean isDisposed() {
    return isDisposed;
  }

  @Override
  protected void getIDsOfNames(String[] names, int[] ids)
      throws HResultException {
    ids[0] = classLoader.getDispId(names[0]);
    if (ids[0] == -1 || names.length >= 2) {
      throw new HResultException(DISP_E_UNKNOWNNAME);
    }
  }

  @Override
  protected Variant invoke(int dispId, int flags, Variant[] params)
      throws HResultException, InvocationTargetException {
    try {
      // Whatever the caller asks for, try to find it via reflection.
      //
      if (dispId == DISPID_MAGIC_GETGLOBALREF && myGlobalRef != 0) {
        // Handle specially.
        //
        return new Variant(myGlobalRef);
      } else if (dispId == 0) {
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          // implicit call -- "m()"
          // not supported -- fall through to unsupported failure
        } else if ((flags & COM.DISPATCH_PROPERTYGET) != 0) {
          // implicit toString -- "'foo' + m"
          return new Variant(getTarget().toString());
        }

      } else if (dispId > 0) {
        if (javaDispatch.isMethod(dispId)) {
          Method method = javaDispatch.getMethod(dispId);
          if ((flags & COM.DISPATCH_METHOD) != 0) {
            // This is a method call.
            return callMethod(classLoader, getTarget(), params, method);
          } else if (flags == COM.DISPATCH_PROPERTYGET) {
            // The function is being accessed as a property.
            IDispatchImpl dispMethod = (IDispatchImpl) classLoader.getMethodDispatch(method);
            if (dispMethod == null) {
              dispMethod = new MethodDispatch(classLoader, method);
              classLoader.putMethodDispatch(method, dispMethod);
            }
            IDispatch disp = new IDispatch(dispMethod.getAddress());
            disp.AddRef();
            return new Variant(disp);
          }
        } else if (javaDispatch.isField(dispId)) {
          Field field = javaDispatch.getField(dispId);
          if (flags == COM.DISPATCH_PROPERTYGET) {
            return SwtOleGlue.convertObjectToVariant(classLoader,
                field.getType(), javaDispatch.getFieldValue(dispId));
          } else if ((flags & (COM.DISPATCH_PROPERTYPUT | COM.DISPATCH_PROPERTYPUTREF)) != 0) {
            javaDispatch.setFieldValue(dispId, JsValueGlue.get(new JsValueIE6(
                params[0]), field.getType(), "Setting field '"
                + field.getName() + "'"));
            return new Variant();
          }
        }
      } else {
        // The specified member id is out of range.
        throw new HResultException(COM.DISP_E_MEMBERNOTFOUND);
      }
    } catch (IllegalArgumentException e) {
      // should never, ever happen
      e.printStackTrace();
      throw new HResultException(e);
    }

    System.err.println("IDispatchProxy cannot be invoked with flags: "
        + Integer.toHexString(flags));
    throw new HResultException(COM.E_NOTSUPPORTED);
  }
}
