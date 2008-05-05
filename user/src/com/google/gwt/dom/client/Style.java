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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Provides programmatic access to properties of the style object.
 * 
 * @see Element#getStyle()
 */
public class Style extends JavaScriptObject {

  protected Style() {
  }

  /**
   * Gets the value of a named property.
   */
  public final native String getProperty(String name) /*-{
    return this[name];
  }-*/;

  /**
   * Sets the value of a named property.
   */
  public final native void setProperty(String name, String value) /*-{
    this[name] = value;
  }-*/;

  /**
   * Sets the value of a named property, in pixels.
   * 
   * This is shorthand for <code>value + "px"</code>.
   */
  public final native void setPropertyPx(String name, int value) /*-{
    this[name] = value + "px";
  }-*/;
}
