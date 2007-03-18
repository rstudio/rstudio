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

import com.google.gwt.dev.shell.mac.LowLevelSaf;

import java.awt.Toolkit;

/**
 * Initializes the low level libraries for Mac.
 */
public class BootStrapPlatform {

  public static void go() {
    // Ensure we were started with -XstartOnFirstThread
    LowLevelSaf.init();
    String[] args = LowLevelSaf.getProcessArgs();
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equalsIgnoreCase("-xstartonfirstthread")) {
        return;
      }
    }
    System.err.println("Invalid launch configuration: -XstartOnFirstThread not specified.");
    System.err.println();
    System.err.println("On Mac OS X, GWT requires that the Java virtual machine be invoked with the");
    System.err.println("-XstartOnFirstThread VM argument.");
    System.err.println();
    System.err.println("Example:");
    System.err.println("  java -XstartOnFirstThread -cp gwt-dev-mac.jar com.google.gwt.dev.GWTShell");
    System.exit(-1);
  }

  /**
   * 
   * This works around a complicated set of OS X SWT/AWT compatibilities.
   * {@link #setSystemProperties()} will typically need to be called first to
   * ensure that CocoaComponent compatability mode is disabled. The constraints
   * of using SWT and AWT together are:
   * 
   * <p>
   * 1 - The SWT event dispatch needs to be running on the main application
   * thread (only possible with -XstartOnFirstThread vm arg).
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
   * {@link #maybeInitializeAWT()} will both be called during the bootstrap
   * process. Command line applications (like 
   * @{link com.google.gwt.dev.GWTCompiler}) avoid eagerly initializing AWT
   * and only call {@link #setSystemProperties()} allowing AWT to be
   * initialized on demand.
   * </p>
   */
  public static void maybeInitializeAWT() {
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
  }

  /**
   * Sets platform specific system properties. Currently, this disables
   * CocoaComponent CompatibilityMode.
   * 
   * <p>
   * NOTE: In GUI applications, {@link #setSystemProperties()} and
   * {@link #maybeInitializeAWT()} will both be called during the bootstrap
   * process. Command line applications (like 
   * @{link com.google.gwt.dev.GWTCompiler}) avoid eagerly initializing AWT
   * and only call {@link #setSystemProperties()} allowing AWT to be
   * initialized on demand.
   * </p>
   */
  public static void setSystemProperties() {
    // Disable CocoaComponent compatibility mode.
    System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode",
        "false");
  }
}
