/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.soyc;

import com.google.gwt.soyc.io.OutputDirectory;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class handles static resources such as CSS and GIF files that support
 * the generated HTML. The resources are expected to be available via this
 * class's class loader.
 */
public class StaticResources {
  /**
   * A list of all static resources. Storing it this way allows the resources to
   * be loaded via a Java class loader, which is often convenient. Class loaders
   * cannot be iterated over.
   */
  private static String[] resourceNames = new String[] {
      "goog.css", "inlay.css", "soyc.css", "images/g_gwt.png",
      "images/up_arrow.png", "images/play-g16.png", "images/play-g16-down.png"};

  public static void emit(OutputDirectory outDir) throws IOException {
    String prefix = StaticResources.class.getPackage().getName().replace('.',
        '/')
        + "/resources/";
    ClassLoader loader = StaticResources.class.getClassLoader();
    for (String resourceName : resourceNames) {
      InputStream in = loader.getResourceAsStream(prefix + resourceName);
      if (in == null) {
        throw new Error("Could not find resource via my class loader: "
            + prefix + resourceName);
      }
      OutputStream out = outDir.getOutputStream(resourceName);
      Utility.streamOut(in, out, 10240);
      in.close();
      out.close();
    }
  }
}
