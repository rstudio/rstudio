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
package com.google.gwt.json.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Represents a JSON boolean value.
 */
public class JSONBoolean extends JSONValue {

  private static final JSONBoolean FALSE = new JSONBoolean(false);
  private static final JSONBoolean TRUE = new JSONBoolean(true);

  /**
   * Gets a reference to the singleton instance representing either
   * <code>true</code> or <code>false</code>.
   * 
   * @param b controls which value to get
   * @return if <code>true</code>, the JSONBoolean instance representing
   *         <code>true</code> is returned; otherwise, the JSONBoolean
   *         instance representing <code>false</code> is returned
   */
  public static JSONBoolean getInstance(boolean b) {
    if (b) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  /**
   * Called from {@link #getUnwrapper()}. 
   */
  private static boolean unwrap(JSONBoolean value) {
    return value.value;
  }

  private final boolean value;

  /*
   * This private constructor is used to build true and false.
   */
  private JSONBoolean(boolean value) {
    this.value = value;
  }

  /**
   * Returns <code>true</code> if this is the instance representing "true",
   * <code>false</code> otherwise.
   */
  public boolean booleanValue() {
    return value;
  }

  /**
   * Returns <code>this</code>, as this is a JSONBoolean.
   */
  @Override
  public JSONBoolean isBoolean() {
    return this;
  }

  /**
   * Returns "true" for the true value, and "false" for the false value.
   */
  @Override
  public String toString() {
    return Boolean.toString(value);
  }

  @Override
  native JavaScriptObject getUnwrapper() /*-{
    return @com.google.gwt.json.client.JSONBoolean::unwrap(Lcom/google/gwt/json/client/JSONBoolean;);
  }-*/;
}
