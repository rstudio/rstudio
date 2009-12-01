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
package com.google.gwt.user.client.ui;

/**
 * An object that implements this interface contains text, which can be set and
 * retrieved using these methods. The object's text can be set either as HTML or
 * as text.
 * 
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * The body of an XML element representing a widget that implements
 * HasHTML will be parsed as HTML and be used in a call to its
 * {@link #setHTML(String)} method.
 * 
 * <p>For example:<pre>
 * &lt;g:PushButton>&lt;b>Click me!&lt;/b>&lt;/g:PushButton>
 * </pre>
 */
public interface HasHTML extends HasText {

  /**
   * Gets this object's contents as HTML.
   * 
   * @return the object's HTML
   */
  String getHTML();

  /**
   * Sets this object's contents via HTML. Use care when setting an object's
   * HTML; it is an easy way to expose script-based security problems. Consider
   * using {@link #setText(String)} whenever possible.
   * 
   * @param html the object's new HTML
   */
  void setHTML(String html);
}
