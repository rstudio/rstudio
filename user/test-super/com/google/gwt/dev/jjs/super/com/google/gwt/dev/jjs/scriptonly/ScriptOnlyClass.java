/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.scriptonly;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This is the web-mode version of this class. It will attempt to call methods
 * not normally available in hosted-mode classes.
 */
@GwtScriptOnly
public class ScriptOnlyClass extends BaseClass {
  public static boolean isInstance(Class<?> test, Object instance) {
    return false;
  }

  /**
   * Test cross-bounday method invocation.
   */
  public void callCallback(AsyncCallback<ScriptOnlyClass> callback) {
    unimplemented();
  }

  /**
   * Access code that's not available in the module's source path.
   */
  public boolean callCodeNotInSourcePath() {
    return this.<Boolean> unimplemented();
  }

  /**
   * Can only do this in hosted mode.
   */
  public String getClassLoaderName() {
    return unimplemented();
  }

  /**
   * Test class-literal access to classes that come from the CCL.
   */
  public Class<?> getWindowClass() {
    return unimplemented();
  }

  @Override
  public boolean isHostedMode() {
    return false;
  }

  private <T> T unimplemented() {
    throw new RuntimeException("unimplemented");
  }
}
