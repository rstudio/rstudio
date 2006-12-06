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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Element;

/**
 * Implementation interface for creating and manipulating focusable elements
 * that aren't naturally focusable in all browsers, such as DIVs.
 */
public class FocusImpl {

  public native void blur(Element elem) /*-{
    elem.blur();
  }-*/;

  public native Element createFocusable() /*-{
    var e = $doc.createElement("DIV");
    e.tabIndex = 0;
    return e;
  }-*/;

  public native void focus(Element elem) /*-{
    elem.focus();
  }-*/;

  public native int getTabIndex(Element elem) /*-{
    return elem.tabIndex;
  }-*/;

  public native void setAccessKey(Element elem, char key) /*-{
    elem.accessKey = key;
  }-*/;

  public native void setTabIndex(Element elem, int index) /*-{
    elem.tabIndex = index;
  }-*/;
}
