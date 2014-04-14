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
package com.google.gwt.dev.shell;

import junit.framework.TestCase;

/**
 * Tests the DevModeSession class.
 */
public class DevModeSessionTest extends TestCase {
  public void testConstructor() {
    String moduleName = "test module name";
    String userAgent = "test user agent";
    DevModeSession session = new DevModeSession(moduleName, userAgent);
    assertEquals("Constructor failed to initialize moduleName", session.getModuleName(), moduleName);
    assertEquals("Constructor failed to initialize userAgent", session.getUserAgent(), userAgent);
  }

  public void testSetSessionForCurrentThread() {
    DevModeSession session = new DevModeSession("test", "test");
    // call method
    DevModeSession.setSessionForCurrentThread(session);
    // verify
    assertTrue(DevModeSession.getSessionForCurrentThread() == session);
    // tear-down
    DevModeSession.setSessionForCurrentThread(null);
  }
}
