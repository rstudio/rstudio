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
package com.google.gwt.dev.jdt;

import com.google.gwt.dev.About;

import java.io.Serializable;

public class ByteCode implements Serializable {

  private static final String systemString = System.getProperty(
      "java.class.path", ".");

  private static final String systemStringAsIdentifier = About.GWT_VERSION
      + "_" + systemString.hashCode();

  /**
   * This method returns the current system identifier, used to detect changes
   * in the environment that would make cached data unusable.
   * 
   * @return the current system identifier, which is sensitive to classpath and
   *         os changes as well as GWT version changes
   */
  public static String getCurrentSystemIdentifier() {
    return systemStringAsIdentifier;
  }

  private final String binaryTypeName;

  private final byte[] bytes;

  private final String location;

  private final String version;

  private final boolean isTransient;

  /**
   * Specifies the bytecode for a given type.
   */
  public ByteCode(String binaryTypeName, byte[] bytes, String location,
      boolean isTransient) {
    this.binaryTypeName = binaryTypeName;
    this.bytes = bytes;
    this.location = location;
    this.version = systemStringAsIdentifier;
    this.isTransient = isTransient;
  }

  public String getBinaryTypeName() {
    return binaryTypeName;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String getLocation() {
    return location;
  }

  public String getSystemIdentifier() {
    return version;
  }

  public boolean isTransient() {
    return isTransient;
  }

  // We explicitly do not set serialVersionUID, as it is generated
  // automatically, and is more sensitive to class file changes than if
  // it were generated manually.
}
