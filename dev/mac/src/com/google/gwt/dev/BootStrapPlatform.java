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

import com.google.gwt.dev.shell.mac.LowLevelSaf;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

/**
 * Initializes the low level libraries for Mac.
 */
public class BootStrapPlatform {

  public static void applyPlatformHacks() {
    setSystemProperties();
    fixContextClassLoaderOnMainThread();
  }

  /**
   * 
   * This works around a complicated set of OS X SWT/AWT compatibilities.
   * {@link #setSystemProperties()} will typically need to be called first to
   * ensure that CocoaComponent compatibility mode is disabled. The constraints
   * of using SWT and AWT together are:
   * 
   * <p>
   * 1 - The SWT event dispatch needs to be running on the main application
   * thread (only possible with -XstartOnFirstThread VM arg).
   * </p>
   * <p>
   * 2 - The first call into AWT must be from the main thread after a SWT
   * display has been initialized.
   * </p>
   * 
   * This method allows the compiler to have a tree logger in a SWT window and
   * allow generators to use AWT for image generation.
   * 
   * <p>
   * NOTE: In GUI applications, {@link #setSystemProperties()} and
   * {@link #initGui()} will both be called during the bootstrap process.
   * Command line applications (like
   * 
   * @{link com.google.gwt.dev.GWTCompiler}) avoid eagerly initializing AWT and
   *        only call {@link #setSystemProperties()} allowing AWT to be
   *        initialized on demand.
   *        </p>
   */
  public static void initGui() {
    GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    Toolkit.getDefaultToolkit();
  }

  public static void initHostedMode() {
    /*
     * The following check must be made before attempting to initialize Safari,
     * or we'll fail with an less-than-helpful UnsatisfiedLinkError.
     */
    if (!isJava5()) {
      System.err.println("You must use a Java 1.5 runtime to use GWT Hosted Mode on Mac OS X.");
      System.exit(-1);
    }

    LowLevelSaf.init();
    // Ensure we were started with -XstartOnFirstThread
    if (!hasStartOnFirstThreadFlag(LowLevelSaf.getProcessArgs())) {
      System.err.println("Invalid launch configuration: -XstartOnFirstThread not specified.");
      System.err.println();
      System.err.println("On Mac OS X, GWT requires that the Java virtual machine be invoked with the");
      System.err.println("-XstartOnFirstThread VM argument.");
      System.err.println();
      System.err.println("Example:");
      System.err.println("  java -XstartOnFirstThread -cp gwt-dev-mac.jar com.google.gwt.dev.GWTShell");
      System.exit(-1);
    }
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

  private static boolean hasStartOnFirstThreadFlag(String[] args) {
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equalsIgnoreCase("-xstartonfirstthread")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine if we're using the Java 1.5 runtime, since the 1.6 runtime is
   * 64-bit.
   */
  private static boolean isJava5() {
    return System.getProperty("java.version").startsWith("1.5");
  }

  /**
   * Sets platform specific system properties. Currently, this disables
   * CocoaComponent CompatibilityMode.
   * 
   * <p>
   * NOTE: In GUI applications, {@link #setSystemProperties()} and
   * {@link #initGui()} will both be called during the bootstrap process.
   * Command line applications (like
   * 
   * @{link com.google.gwt.dev.GWTCompiler}) avoid eagerly initializing AWT and
   *        only call {@link #setSystemProperties()} allowing AWT to be
   *        initialized on demand.
   *        </p>
   */
  private static void setSystemProperties() {
    // Disable CocoaComponent compatibility mode.
    System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode",
        "false");
  }
}
