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
package com.google.gwt.i18n.client;

import java.util.Map;

/**
 * Like {@link com.google.gwt.i18n.client.Constants}, a tag interface that
 * facilitates locale-sensitive, compile-time binding of constant values
 * supplied from properties files with the added ability to look up constants at
 * runtime with a string key.
 * 
 * <p>
 * <code>ConstantsWithLookup</code> extends
 * {@link com.google.gwt.i18n.client.Constants} and is identical in behavior,
 * adding only a family of special-purpose lookup methods such as
 * {@link ConstantsWithLookup#getString(String)}.
 * </p>
 * 
 * <p>
 * It is generally preferable to extend <code>Constants</code> rather than
 * <code>ConstantsWithLookup</code> because <code>ConstantsWithLookup</code>
 * forces all constants to be retained in the compiled script, preventing the
 * GWT compiler from pruning unused constant accessors.
 * </p>
 * 
 * <h3>Required Module</h3>
 * Modules that use this interface should inherit
 * <code>com.google.gwt.i18n.I18N</code>.
 * 
 * {@gwt.include com/google/gwt/examples/i18n/InheritsExample.gwt.xml}
 * 
 * <h3>Note</h3>
 * You should not directly implement this interface or interfaces derived from
 * it since an implementation is generated automatically when message interfaces
 * are created using {@link com.google.gwt.core.client.GWT#create(Class)}.
 * 
 * @see com.google.gwt.i18n.client.Constants
 */
public interface ConstantsWithLookup extends Constants {
  /**
   * Look up <code>boolean</code> by method name.
   * 
   * @param methodName method name
   * @return boolean returned by method
   */
  public boolean getBoolean(String methodName);

  /**
   * Look up <code>double</code> by method name.
   * 
   * @param methodName method name
   * @return double returned by method
   */
  public double getDouble(String methodName);

  /**
   * Look up <code>float</code> by method name.
   * 
   * @param methodName method name
   * @return float returned by method
   */
  public float getFloat(String methodName);

  /**
   * Look up <code>int</code> by method name.
   * 
   * @param methodName method name
   * @return int returned by method
   */
  public int getInt(String methodName);

  /**
   * Look up <code>Map</code> by method name.
   * 
   * @param methodName method name
   * @return Map returned by method
   */
  public Map getMap(String methodName);

  /**
   * Look up <code>String</code> by method name.
   * 
   * @param methodName method name
   * @return String returned by method
   */
  public String getString(String methodName);

  /**
   * Look up <code>String[]</code> by method name.
   * 
   * @param methodName method name
   * @return String[] returned by method
   */
  public String[] getStringArray(String methodName);
}
