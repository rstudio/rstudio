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
package com.google.gwt.i18n.client;

import java.util.Map;
import java.util.MissingResourceException;

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
   * @throws MissingResourceException if methodName is not valid
   */
  boolean getBoolean(String methodName) throws MissingResourceException;

  /**
   * Look up <code>double</code> by method name.
   * 
   * @param methodName method name
   * @return double returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  double getDouble(String methodName) throws MissingResourceException;

  /**
   * Look up <code>float</code> by method name.
   * 
   * @param methodName method name
   * @return float returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  float getFloat(String methodName) throws MissingResourceException;

  /**
   * Look up <code>int</code> by method name.
   * 
   * @param methodName method name
   * @return int returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  int getInt(String methodName) throws MissingResourceException;

  /**
   * Look up <code>Map</code> by method name.
   * 
   * @param methodName method name
   * @return Map returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  Map<String, String> getMap(String methodName) throws MissingResourceException;

  /**
   * Look up <code>String</code> by method name.
   * 
   * @param methodName method name
   * @return String returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  String getString(String methodName) throws MissingResourceException;

  /**
   * Look up <code>String[]</code> by method name.
   * 
   * @param methodName method name
   * @return String[] returned by method
   * @throws MissingResourceException if methodName is not valid
   */
  String[] getStringArray(String methodName) throws MissingResourceException;
}
