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
package com.google.gwt.dev.generator;

/**
 * Utility methods for generators to use.
 */
public class GenUtil {

  /**
   * If <code>true</code>, the default, generators should warn when a client
   * uses old-style javadoc metadata rather than Java 1.5 annotations. If
   * <code>false</code>, generators should not warn.
   */
  public static boolean warnAboutMetadata() {
    return System.getProperty("gwt.nowarn.metadata") != null;
  }

  /**
   * Not instantiable.
   */
  private GenUtil() {
  }
}
