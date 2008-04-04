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
 * Generic embedded object.
 * 
 * Note: In principle, all properties on the object element are read-write but
 * in some environments some properties may be read-only once the underlying
 * object is instantiated.
 * 
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-OBJECT
 */
public class ObjectElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ObjectElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("object");
    return (ObjectElement) elem;
  }

  protected ObjectElement() {
  }

  /**
   * Applet class file.
   */
  public final native String getCode() /*-{
     return this.code;
   }-*/;

  /**
   * The document this object contains, if there is any and it is available, or
   * null otherwise.
   */
  public final native Document getContentDocument() /*-{
     return this.contentDocument;
   }-*/;

  /**
   * A URI specifying the location of the object's data.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-data
   */
  public final native String getData() /*-{
     return this.data;
   }-*/;

  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */
  public final native FormElement getForm() /*-{
     return this.form;
   }-*/;

  /**
   * Override height.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG
   */
  public final native String getHeight() /*-{
     return this.height;
   }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native int getTabIndex() /*-{
     return this.tabIndex;
   }-*/;

  /**
   * Content type for data downloaded via data attribute.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-type-OBJECT
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * Override width.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG
   */
  public final native String getWidth() /*-{
     return this.width;
   }-*/;

  /**
   * Applet class file.
   */
  public final native void setCode(String code) /*-{
     this.code = code;
   }-*/;

  /**
   * A URI specifying the location of the object's data.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-data
   */
  public final native void setData(String data) /*-{
     this.data = data;
   }-*/;

  /**
   * Override height.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG
   */
  public final native void setHeight(String height) /*-{
     this.height = height;
   }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native void setTabIndex(int tabIndex) /*-{
     this.tabIndex = tabIndex;
   }-*/;

  /**
   * Content type for data downloaded via data attribute.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-type-OBJECT
   */
  public final native void setType(String type) /*-{
     this.type = type;
   }-*/;

  /**
   * Use client-side image map.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap
   */
  public final native void setUseMap(boolean useMap) /*-{
     this.useMap = useMap;
   }-*/;

  /**
   * Override width.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG
   */
  public final native void setWidth(String width) /*-{
     this.width = width;
   }-*/;

  /**
   * Use client-side image map.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap
   */
  public final native boolean useMap() /*-{
     return this.useMap;
   }-*/;
}
