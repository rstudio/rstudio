/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.safehtml.client;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * An object that implements this interface contains text with HTML markup,
 * which can be set with the Cross-Site-Scripting-safe HTML markup encapsulated
 * in a {@link com.google.gwt.safehtml.shared.SafeHtml} object.
 */
public interface HasSafeHtml {

  /**
   * Sets this object's contents via known-safe HTML.
   *
   * <p>
   * The object will behave exactly the same as when a widget's
   * {@link com.google.gwt.user.client.ui.HasHTML#setHTML(String)} method is
   * invoked; however the {@link SafeHtml} passed to this method observes the
   * contract that it can be used in an HTML context without causing unsafe
   * script execution. Thus, unlike
   * {@link com.google.gwt.user.client.ui.HasHTML#setHTML(String)}, using this
   * method cannot result in Cross-Site Scripting security vulnerabilities.
   *
   * @param html the object's new HTML, represented as a {@link SafeHtml} object
   */
  void setHTML(SafeHtml html);
}
