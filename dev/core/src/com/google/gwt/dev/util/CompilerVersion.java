/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.thirdparty.guava.common.hash.Hashing;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URLConnection;

/**
 * Utility for uniquely identifying the current compiler version.
 */
public class CompilerVersion {

  private static String versionHash;

  /**
   * Calculates and returns a hash to uniquely identify the current compiler version if possible.
   * <p>
   * If the compiler jar can not be found then a random hash is returned.
   */
  public static synchronized String getHash() {
    if (versionHash == null) {
      versionHash = "unknown-version-" + StringUtils.toHexString(
          Md5Utils.getMd5Digest(Long.toString((long) (Long.MAX_VALUE * Math.random()))));

      try {
        URLConnection urlConnection = JNode.class.getResource("JNode.class").openConnection();
        if (urlConnection instanceof JarURLConnection) {
          String gwtdevJar = ((JarURLConnection) urlConnection).getJarFile().getName();
          versionHash = Files.hash(new File(gwtdevJar), Hashing.sha1()).toString();
        } else {
          System.err.println("Could not find the GWT compiler jarfile. "
              + "Serialization errors might occur when accessing the persistent unit cache.");
        }
      } catch (IOException e) {
        System.err.println("Could not compute the hash for the GWT compiler jarfile."
            + "Serialization errors might occur when accessing the persistent unit cache.");
      }
    }
    return versionHash;
  }
}
