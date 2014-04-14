/*
 * Copyright 2008 Google Inc.
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

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.Locale;

/**
 * Initializes platform stuff.
 */
public class BootStrapPlatform {

  public static void applyPlatformHacks() {
    if (isMac()) {
      setSystemProperties();
      fixContextClassLoaderOnMainThread();
    }
  }

  /**
   * This method allows the compiler to have a tree logger in an AWT window and
   * allow generators to use AWT for image generation.
   */
  public static void initGui() {
    if (isMac() && !GraphicsEnvironment.isHeadless()) {
      // This has to be set before any UI components are initialized
      System.setProperty("com.apple.mrj.application.apple.menu.about.name",
          "Development Mode");
      GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      Toolkit.getDefaultToolkit();
    }
  }

  public static void initHostedMode() {
    // nothing required
  }

  /**
   * Return true if we are running on a Mac.
   */
  public static boolean isMac() {
    String lcOSName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    return lcOSName.startsWith("mac ");
  }

  /**
   * This works around apple radr:5569300. When -XstartOnFirstThread is passed
   * as a jvm argument, the main thread returns null for
   * {@link Thread#getContextClassLoader()}.
   */
  private static void fixContextClassLoaderOnMainThread() {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      Thread.currentThread().setContextClassLoader(
          BootStrapPlatform.class.getClassLoader());
    }
  }

  /**
   * Sets platform specific system properties. Currently, this disables
   * CocoaComponent CompatibilityMode.
   */
  private static void setSystemProperties() {
    // Disable CocoaComponent compatibility mode.
    System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode",
        "false");
  }
}
