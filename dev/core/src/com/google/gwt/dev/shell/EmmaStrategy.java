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
package com.google.gwt.dev.shell;

import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides various strategies for emma integration based on runtime detection.
 */
abstract class EmmaStrategy {

  private static class NoEmmaStrategy extends EmmaStrategy {
    @Override
    public byte[] getEmmaClassBytes(byte[] classBytes, String slashedName,
        long unitLastModified) {
      return classBytes;
    }
  }

  private static class PreinstrumentedEmmaStrategy extends EmmaStrategy {
    @Override
    public byte[] getEmmaClassBytes(byte[] classBytes, String slashedName,
        long unitLastModified) {
      // Check for an existing class on the classpath.
      URL url = Thread.currentThread().getContextClassLoader().getResource(
          slashedName + ".class");
      if (url != null) {
        // We found it on the class path.
        try {
          URLConnection conn = url.openConnection();
          if (conn.getLastModified() >= unitLastModified) {
            // It's as new as the source file, let's use it.
            byte[] result = Util.readURLConnectionAsBytes(conn);
            if (result != null) {
              return result;
            }
            // Fall through.
          }
          // Fall through.
        } catch (IOException ignored) {
          // Fall through.
        }
      }

      // Just return what we got.
      return classBytes;
    }
  }

  /**
   * Classname for Emma's RT, to enable bridging.
   */
  public static final String EMMA_RT_CLASSNAME = "com.vladium.emma.rt.RT";

  /**
   * Gets the emma classloading strategy.
   */
  public static EmmaStrategy get(boolean emmaIsAvailable) {
    /*
     * Theoretically, emmarun could be using an instrumented ClassLoader, but in
     * practice we haven't been able to make GWT run at all in this case.
     */
    if (!emmaIsAvailable) {
      return new NoEmmaStrategy();
    } else {
      return new PreinstrumentedEmmaStrategy();
    }
  }

  public abstract byte[] getEmmaClassBytes(byte[] classBytes,
      String slashedName, long unitLastModified);

}
