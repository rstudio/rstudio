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

/**
 * Parameters fed to the OBJECT element.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-PARAM">W3C HTML Specification</a>
 */
@TagName(ParamElement.TAG)
public class ParamElement extends Element {

  public static final String TAG = "param";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ParamElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (ParamElement) elem;
  }

  protected ParamElement() {
  }

  /**
   * The name of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-name-PARAM">W3C HTML Specification</a>
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * The value of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-value-PARAM">W3C HTML Specification</a>
   */
  public final native String getValue() /*-{
     return this.value;
   }-*/;

  /**
   * The name of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-name-PARAM">W3C HTML Specification</a>
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * The value of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-value-PARAM">W3C HTML Specification</a>
   */
  public final native void setValue(String value) /*-{
     this.value = value;
   }-*/;
}
