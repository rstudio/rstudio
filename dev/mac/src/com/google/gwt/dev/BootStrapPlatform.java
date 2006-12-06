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
package com.google.gwt.dev;

import com.google.gwt.dev.shell.mac.LowLevelSaf;

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

}
