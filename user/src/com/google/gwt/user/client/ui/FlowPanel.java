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

import com.google.gwt.user.client.DOM;

/**
 * A panel that formats its child widgets using the default HTML layout
 * behavior.
 * 
 * <p>
 * <img class='gallery' src='FlowPanel.png'/>
 * </p>
 */
public class FlowPanel extends ComplexPanel implements IndexedPanel {

  /**
   * Creates an empty flow panel.
   */
  public FlowPanel() {
    setElement(DOM.createDiv());
  }

  /**
   * Adds a new child widget to the panel.
   * 
   * @param w the widget to be added
   */
  public void add(Widget w) {
    super.add(w, getElement());
  }

  public Widget getWidget(int index) {
    return getChildren().get(index);
  }

  public int getWidgetCount() {
    return getChildren().size();
  }

  public int getWidgetIndex(Widget child) {
    return getChildren().indexOf(child);
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }
}
