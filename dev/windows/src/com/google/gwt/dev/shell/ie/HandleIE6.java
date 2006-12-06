// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.Handle;

import org.eclipse.swt.internal.ole.win32.IDispatch;

class HandleIE6 extends Handle {

  static {
    // put myself in Handle's sImpl field
    new HandleIE6();
  }
  
  /**
   * Not instantiable.
   */
  private HandleIE6() {
  }

  public static Object createHandle(Class type, int ptr) {
    return Handle.createHandle(type, ptr);
  }

  public static IDispatch getDispatchFromHandle(Object handle) {
    int ptr = Handle.getPtrFromHandle(handle);
    return new IDispatch(ptr);
  }

  protected void lockPtr(int ptr) {
    SwtOleGlue.addRefInt(ptr);
  }

  protected void unlockPtr(int ptr) {
    SwtOleGlue.releaseInt(ptr);
  }

}
