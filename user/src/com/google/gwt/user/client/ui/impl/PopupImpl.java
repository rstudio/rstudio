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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * Implementation class used by {@link com.google.gwt.user.client.ui.PopupPanel}.
 */
public class PopupImpl {

  public Element createElement() {
    return Document.get().createDivElement();
  }

  public Element getContainerElement(Element popup) {
    return popup;
  }

  public Element getStyleElement(Element popup) {
    return popup.getParentElement();
  }

  /**
   * @param popup the popup
   */
  public void onHide(Element popup) {
  }

  /**
   * @param popup the popup
   */
  public void onShow(Element popup) {
  }

  /**
   * @param popup the popup
   * @param rect the clip rect
   */
  public void setClip(Element popup, String rect) {
    popup.getStyle().setProperty("clip", rect);
  }

  /**
   * @param popup the popup
   * @param visible true if visible
   */
  public void setVisible(Element popup, boolean visible) {
  }
}
