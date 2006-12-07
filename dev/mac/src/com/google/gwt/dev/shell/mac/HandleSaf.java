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

import com.google.gwt.dev.shell.Handle;

class HandleSaf extends Handle {

  static {
    // put myself in Handle's sImpl field
    new HandleSaf();
  }

  public static Object createHandle(Class type, int ptr) {
    return Handle.createHandle(type, ptr);
  }

  public static int getJSObjectFromHandle(Object o) {
    return getPtrFromHandle(o);
  }

  /**
   * Not instantiable.
   */
  private HandleSaf() {
  }

  protected void lockPtr(int ptr) {
    LowLevelSaf.gcLock(ptr);
  }

  protected void unlockPtr(int ptr) {
    LowLevelSaf.gcUnlock(ptr);
  }

}
