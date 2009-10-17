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

import com.google.gwt.dev.util.Util;

/**
 * Executable class provides help to users who run the jar by indicating the
 * enclosed classes that are executable.
 */
public class GWTMain {

  public static void main(String args[]) {
    String aboutText = Util.getFileFromInstallPath("about.txt");
    if (aboutText != null) {
      System.err.println(aboutText);
    } else {
      System.err.println(About.getGwtVersion());
    }
    System.err.println("Available main classes:");
    System.err.println(addSpaces(DevMode.class.getName(),
        "runs the development shell"));
    System.err.println(addSpaces(Compiler.class.getName(),
        "compiles a GWT module"));
  }

  private static String addSpaces(String first, String second) {
    StringBuffer sb = new StringBuffer();
    sb.append(' ');
    sb.append(first);
    for (int i = sb.length(); i < 40; ++i) {
      sb.append(' ');
    }
    sb.append(second);
    return sb.toString();
  }

}
