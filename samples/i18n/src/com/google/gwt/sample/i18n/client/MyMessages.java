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
package com.google.gwt.sample.i18n.client;

/**
 * Messages used by the i18n sample.
 */
public interface MyMessages extends com.google.gwt.i18n.client.Messages {

  /**
   * Translated "Remaining space in your account: {0} MB ".
   * 
   * @param size size
   * @return translated "Remaining space in your account: {0} MB "
   * @gwt.key info
   */
  String info(int size);

  /**
   * Translated "''{0}'' cannot be parsed as an integer. ".
   * 
   * @param text text
   * @return translated "''{0}'' cannot be parsed as an integer. "
   * @gwt.key intParseError
   */
  String intParseError(String text);

  /**
   * Translated "''{0}'' is a required field.".
   * 
   * @param text text
   * @return translated "''{0}'' is a required field."
   * @gwt.key requiredField
   */
  String requiredField(String text);

  /**
   * Translated "You only have security clearance {1}, so you cannot access
   * ''{0}''. ".
   * 
   * @param resource resource
   * @param security security
   * @return translated "You only have security clearance {1}, so you cannot
   *         access ''{0}''. "
   * @gwt.key permission
   */
  String permission(String resource, String security);
}
