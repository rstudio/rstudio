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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Element;

/**
 * Implementation interface for creating and manipulating focusable elements
 * that aren't naturally focusable in all browsers, such as DIVs.
 */
public class FocusImpl {

  private static FocusImpl implPanel = GWT.create(FocusImpl.class);

  /**
   * This instance may not be a {@link FocusImplStandard}, because that special
   * case is only needed for things that aren't naturally focusable on some
   * browsers, such as DIVs. This exact class works for truly focusable widgets
   * on those browsers.
   * 
   * The compiler will optimize out the conditional.
   */
  private static FocusImpl implWidget = (implPanel instanceof FocusImplStandard)
      ? new FocusImpl() : implPanel;

  /**
   * Returns the focus implementation class for creating and manipulating
   * focusable elements that aren't naturally focusable in all browsers, such as
   * DIVs.
   */
  public static FocusImpl getFocusImplForPanel() {
    return implPanel;
  }

  /**
   * Returns the focus implementation class for manipulating focusable elements
   * that are naturally focusable in all browsers, such as text boxes.
   */
  public static FocusImpl getFocusImplForWidget() {
    return implWidget;
  }

  /**
   * Not externally instantiable or extensible.
   */
  FocusImpl() {
  }

  public void blur(Element elem) {
    elem.blur();
  }

  public Element createFocusable() {
    Element e = Document.get().createDivElement().cast();
    e.setTabIndex(0);
    return e;
  }

  public void focus(Element elem) {
    elem.focus();
  }

  public int getTabIndex(Element elem) {
    return elem.getTabIndex();
  }

  public native void setAccessKey(Element elem, char key) /*-{
    elem.accessKey = String.fromCharCode(key);
  }-*/;

  public void setTabIndex(Element elem, int index) {
    elem.setTabIndex(index);
  }
}
