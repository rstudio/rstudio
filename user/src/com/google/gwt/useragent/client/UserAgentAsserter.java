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
   * Replacement for UserAgentAsserter to disable it.
   */
  public static class UserAgentAsserterDisabled implements EntryPoint {
    @Override
    public void onModuleLoad() { /* Empty - no assertions */}
  }

  @Override
  public void onModuleLoad() {
    scheduleUserAgentCheck();
  }

  private static native void scheduleUserAgentCheck() /*-{
    // Keeping minimal dependency to reduce risk of problems due to use of wrong permutation:
    $wnd.setTimeout($entry(@com.google.gwt.useragent.client.UserAgentAsserter::assertCompileTimeUserAgent()));
  }-*/;

  private static void assertCompileTimeUserAgent() {
    UserAgent impl = GWT.create(UserAgent.class);

    String compileTimeValue = impl.getCompileTimeValue();
    String runtimeValue = impl.getRuntimeValue();

    if (!compileTimeValue.equals(runtimeValue)) {
      // Let it escape and get handled by UCEH:
      throw new UserAgentAssertionError(compileTimeValue, runtimeValue);
    }
  }

  /**
   * An error object that indicates that the user agent detected at runtime did
   * not match the user agent that the module expected to find.
   */
  public static class UserAgentAssertionError extends AssertionError {
    /**
     * Default constructor for serialization.
     */
    public UserAgentAssertionError() {
    }

    /**
     * Creates an error with the given expected and actual user.agent value.
     *
     * @param compileTimeValue the compile time user.agent value, either in this
     *                         permutation or soft permutation
     * @param runtimeValue the detected user.agent value after the module started
     */
    public UserAgentAssertionError(String compileTimeValue, String runtimeValue) {
      super("Possible problem with your *.gwt.xml module file.\n"
              + "The compile time user.agent value (" + compileTimeValue + ") "
              + "does not match the runtime user.agent value (" + runtimeValue + ").\n"
              + "Expect more errors.");
    }
  }
}
