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
import com.google.gwt.dev.jjs.server.AccessedByScriptOnlyClass;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This is the hosted-mode version of this class. It will attempt to call
 * methods not normally available in hosted-mode classes.
 */
@GwtScriptOnly
public class ScriptOnlyClass extends BaseClass {
  /*
   * NB: If you see errors from the GWT compiler while trying to compile this
   * type, make sure that the user/test-super folder is on the classpath.
   */
  public static boolean isInstance(Class<?> test, Object instance) {
    return test.isInstance(instance);
  }

  /**
   * Test cross-boundary method invocation.
   */
  public void callCallback(AsyncCallback<ScriptOnlyClass> callback) {
    assert callback != null;
    assert callback.getClass().getClassLoader() == this.getClass().getClassLoader().getParent();
    callback.onSuccess(this);
  }

  /**
   * Access code that's not available in the module's source path.
   */
  public boolean callCodeNotInSourcePath() {
    return AccessedByScriptOnlyClass.getBool();
  }

  /**
   * Can only do this in Development Mode.
   */
  public String getClassLoaderName() {
    return getClass().getClassLoader().getClass().getName();
  }

  /**
   * Test class-literal access to classes that come from the CCL.
   */
  public Class<?> getWindowClass() {
    return Window.class;
  }

  @Override
  public boolean isHostedMode() {
    return true;
  }
}
