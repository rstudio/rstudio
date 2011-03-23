/*
 * Copyright 2011 Google Inc.
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
 * IE9 based implementation of {@link com.google.gwt.user.client.impl.DOMImplStandardBase}.
 */
class DOMImplIE9 extends DOMImplStandardBase {
  @Override
  public int getAbsoluteLeft(Element elem) {
    Document doc = elem.getOwnerDocument();
    return getBoundingClientRectLeft(elem) + doc.getScrollLeft();
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    Document doc = elem.getOwnerDocument();
    return getBoundingClientRectTop(elem) + doc.getScrollTop();
  }

  public native void selectRemoveOption(SelectElement select, int index) /*-{
    try {
      // IE9 throws if elem at index is an optgroup
      select.remove(index);
    } catch(e) {
      select.removeChild(select.childNodes[index]);
    }
  }-*/;

  protected native int getBoundingClientRectLeft(Element elem) /*-{
  // getBoundingClientRect() throws a JS exception if the elem is not attached
  // to the document, so we wrap it in a try/catch block
  try {
    return elem.getBoundingClientRect().left;
  } catch (e) {
    // if not attached return 0
    return 0;
  }
}-*/;

  protected native int getBoundingClientRectTop(Element elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the document, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().top;
    } catch (e) {
      // if not attached return 0
      return 0;
    }
  }-*/;
}
