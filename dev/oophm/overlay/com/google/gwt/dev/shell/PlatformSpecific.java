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
package com.google.gwt.dev.shell;

import java.lang.reflect.Constructor;

/**
 * Performs platform-specific class selection.
 */
public class PlatformSpecific {

  /**
   * All of these classes must extend CheckForUpdates.
   */
  private static final String[] updaterClassNames = new String[] {
  // "com.google.gwt.dev.shell.ie.CheckForUpdatesIE6",
  // "com.google.gwt.dev.shell.moz.CheckForUpdatesMoz",
  // "com.google.gwt.dev.shell.mac.CheckForUpdatesSaf"
  };

  @SuppressWarnings("unchecked")
  // Class.forName
  public static CheckForUpdates createUpdateChecker() {
    try {
      for (int i = 0; i < updaterClassNames.length; i++) {
        try {
          Class<CheckForUpdates> clazz = (Class<CheckForUpdates>) Class.forName(updaterClassNames[i]);
          Constructor<CheckForUpdates> ctor = clazz.getDeclaredConstructor(new Class[] {});
          CheckForUpdates checker = ctor.newInstance(new Object[] {});
          return checker;
        } catch (ClassNotFoundException e) {
          // keep trying
        }
      }
    } catch (Throwable e) {
      // silently ignore any errors
    }
    return null;
  }
}
