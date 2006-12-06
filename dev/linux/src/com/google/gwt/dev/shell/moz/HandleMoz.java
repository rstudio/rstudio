// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.Handle;

class HandleMoz extends Handle {

  static {
    // put myself in Handle's sImpl field
    new HandleMoz();
  }

  /**
   * Not instantiable.
   */
  private HandleMoz() {
  }

  public static Object createHandle(Class type, int ptr) {
    return Handle.createHandle(type, ptr);
  }

  protected void lockPtr(int ptr) {
    SwtGeckoGlue.addRefInt(ptr);
  }

  protected void unlockPtr(int ptr) {
    SwtGeckoGlue.releaseInt(ptr);
  }

  public static int getJSObjectFromHandle(Object o) {
    return LowLevelMoz.unwrapJSObject(getPtrFromHandle(o));
  }

}
