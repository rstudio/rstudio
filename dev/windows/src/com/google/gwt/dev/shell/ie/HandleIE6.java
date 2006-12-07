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

import com.google.gwt.dev.shell.Handle;

import org.eclipse.swt.internal.ole.win32.IDispatch;

class HandleIE6 extends Handle {

  static {
    // put myself in Handle's sImpl field
    new HandleIE6();
  }

  public static Object createHandle(Class type, int ptr) {
    return Handle.createHandle(type, ptr);
  }

  public static IDispatch getDispatchFromHandle(Object handle) {
    int ptr = Handle.getPtrFromHandle(handle);
    return new IDispatch(ptr);
  }

  /**
   * Not instantiable.
   */
  private HandleIE6() {
  }

  protected void lockPtr(int ptr) {
    SwtOleGlue.addRefInt(ptr);
  }

  protected void unlockPtr(int ptr) {
    SwtOleGlue.releaseInt(ptr);
  }

}
