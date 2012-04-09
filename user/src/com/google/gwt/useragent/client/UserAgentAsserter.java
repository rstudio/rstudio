/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.useragent.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * Helper class, which, during startup, asserts that the specified user.agent
 * selection property value indeed matches the expected value for this browser /
 * user agent, thus avoid long hours debugging strange error messages when a
 * single user agent compile, typically created for testing purposes, ends up
 * being executed in the wrong browser.
 */
public class UserAgentAsserter implements EntryPoint {

  /**
   * Interface to provide both the compile time and runtime
   * <code>user.agent</code> selection property value.
   */
  interface UserAgentProperty {
    String getCompileTimeValue();

    String getRuntimeValue();

    boolean getUserAgentRuntimeWarning();
  }

  /**
   * Default {@link UserAgentProperty} implementation used when
   * {@code user.agent.runtimeWarning} is {@code false}.
   */
  static class UserAgentPropertyDisabled implements UserAgentProperty {
    @Override
    public String getCompileTimeValue() {
      return null;
    }

    @Override
    public String getRuntimeValue() {
      return null;
    }

    @Override
    public boolean getUserAgentRuntimeWarning() {
      return false;
    }
  }

  @Override
  public void onModuleLoad() {
    UserAgentProperty impl = GWT.create(UserAgentProperty.class);
    if (!impl.getUserAgentRuntimeWarning()) {
      return;
    }
    
    String compileTimeValue = impl.getCompileTimeValue();
    String runtimeValue = impl.getRuntimeValue();

    if (!compileTimeValue.equals(runtimeValue)) {
      displayMismatchWarning(runtimeValue, compileTimeValue);
    }
  }

  /**
   * Implemented as a JSNI method to avoid potentially using any user agent
   * specific deferred binding code, since this method is called precisely when
   * we're somehow executing code from the wrong user.agent permutation.
   */
  private native void displayMismatchWarning(String runtimeValue,
      String compileTimeValue) /*-{
    $wnd.alert("ERROR: Possible problem with your *.gwt.xml module file."
        + "\nThe compile time user.agent value (" + compileTimeValue
        + ") does not match the runtime user.agent value (" + runtimeValue
        + "). Expect more errors.\n");
  }-*/;
}
