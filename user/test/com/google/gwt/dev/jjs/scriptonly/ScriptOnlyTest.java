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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Tests various aspects of using user-defined bridge classes in Development
 * Mode.
 */
public class ScriptOnlyTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.ScriptOnlyTest";
  }

  public void testClassSemantics() {
    Object o = new ScriptOnlyClass();
    assertTrue(o instanceof ScriptOnlyClass);
    assertTrue(o instanceof BaseClass);

    assertEquals(!GWT.isScript(),
        ScriptOnlyClass.isInstance(BaseClass.class, o));

    // Test cast
    assertEquals(!GWT.isScript(), ((BaseClass) o).isHostedMode());
  }

  public void testUserBridgeClass() {
    final ScriptOnlyClass b = new ScriptOnlyClass();
    if (GWT.isScript()) {
      // Just make sure the super-source version is used in Production Mode
      assertFalse(b.isHostedMode());
      return;
    }

    // Make sure the right version of the class was loaded
    assertTrue(b.isHostedMode());

    // Is the sub-loader delegating to our CCL?
    assertSame(Window.class, b.getWindowClass());

    // Try something you can't do in Production Mode (JRE code)
    assertNotNull(b.getClassLoaderName());

    // Try something you can't do in Prodiction Mode ("server" code)
    assertTrue(b.callCodeNotInSourcePath());

    b.callCallback(new AsyncCallback<ScriptOnlyClass>() {
      public void onFailure(Throwable caught) {
        fail(caught.getMessage());
      }

      public void onSuccess(ScriptOnlyClass result) {
        assertSame(b, result);
      }
    });
  }
}
