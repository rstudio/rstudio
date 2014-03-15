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

/**
 * Provides public method for convenience in unit tests. Allows access of
 * package-private member
 * {@link DevModeSession#setSessionForCurrentThread(DevModeSession)}.
 */
public class DevModeSessionTestUtil {
  public static DevModeSession createSession(String moduleName, String userAgent,
      boolean setForCurrentThread) {
    DevModeSession session = new DevModeSession(moduleName, userAgent);
    if (setForCurrentThread) {
      DevModeSession.setSessionForCurrentThread(session);
    }
    return session;
  }
}
