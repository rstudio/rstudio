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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * A simple panel that {@link ProvidesResize} to its one child.
 */
public class SimpleLayoutPanel extends SimplePanel implements RequiresResize,
    ProvidesResize {

  private Layer layer;
  private final Layout layout;

  public SimpleLayoutPanel() {
    layout = new Layout(getElement());
  }

  @Override
  public void onResize() {
    if (widget instanceof RequiresResize) {
      ((RequiresResize) widget).onResize();
    }
  }

  @Override
  public boolean remove(Widget w) {
    // Validate.
    if (widget != w) {
      return false;
    }

    // Orphan.
    try {
      orphan(w);
    } finally {
      // Physical detach.
      layout.removeChild(layer);
      layer = null;

      // Logical detach.
      widget = null;
    }
    return true;
  }

  @Override
  public void setWidget(Widget w) {
    // Validate
    if (w == widget) {
      return;
    }

    // Detach new child.
    if (w != null) {
      w.removeFromParent();
    }

    // Remove old child.
    if (widget != null) {
      remove(widget);
    }

    // Logical attach.
    widget = w;

    if (w != null) {
      // Physical attach.
      layer = layout.attachChild(widget.getElement(), widget);
      layer.setTopHeight(0.0, Unit.PX, 100.0, Unit.PCT);
      layer.setLeftWidth(0.0, Unit.PX, 100.0, Unit.PCT);

      adopt(w);

      // Update the layout.
      layout.layout();
      onResize();
    }
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    layout.onAttach();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    layout.onDetach();
  }
}
