/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.dev.shell.moz.MozillaInstall;

/**
 * Initializes low-level libraries for linux.
 */
public class BootStrapPlatform {

  public static void applyPlatformHacks() {
    // nothing to do
  }

  /**
   * Find a usable Mozilla installation and load it. Fail immediately, logging
   * to stderr and exiting with a failure code, if we are unable to find or load
   * it. If successful, store the loaded path in the property swt.mozilla.path
   * so SWT's Browser object can use it.
   */
  public static void init() {
    String home = System.getenv("HOME");
    if (home == null || home.length() == 0) {
      System.err.println("The HOME environment variable must be defined.");
      System.exit(1);
    }
    MozillaInstall mozInstall = MozillaInstall.find();
    if (mozInstall == null) {
      System.err.println("** Unable to find a usable Mozilla install **");
      System.err.println("You may specify one in mozilla-hosted-browser.conf, "
          + "see comments in the file for details.");
      System.exit(1);
    }
    try {
      mozInstall.load();
    } catch (UnsatisfiedLinkError e) {
      System.err.println("** Unable to load Mozilla for hosted mode **");
      e.printStackTrace();
      System.exit(1);
    }
    String mozillaPath = mozInstall.getPath();
    System.setProperty("swt.mozilla.path", mozillaPath);
  }

  public static void maybeInitializeAWT() {
    // nothing to do
  }
}
