/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.layout.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * This implementation is used on IE7 and IE8. Unlike {@link LayoutImpl}, it
 * converts all values to pixels before setting them. This is necessary because
 * these browsers incorrectly calculate the relative sizes and positions of CSS
 * properties specified in certain units (e.g., when the value of an 'em' is
 * non-integral in pixels).
 */
public class LayoutImplIE8 extends LayoutImpl {

  public void layout(Layer layer) {
    Style style = layer.container.getStyle();

    if (layer.setLeft) {
      setValue(layer, "left", layer.left, layer.leftUnit, false, false);
    } else {
      style.clearLeft();
    }
    if (layer.setRight) {
      setValue(layer, "right", layer.right, layer.rightUnit, false, false);
    } else {
      style.clearRight();
    }
    if (layer.setTop) {
      setValue(layer, "top", layer.top, layer.topUnit, true, false);
    } else {
      style.clearTop();
    }
    if (layer.setBottom) {
      setValue(layer, "bottom", layer.bottom, layer.bottomUnit, true, false);
    } else {
      style.clearBottom();
    }
    if (layer.setWidth) {
      setValue(layer, "width", layer.width, layer.widthUnit, false, true);
    } else {
      style.clearWidth();
    }
    if (layer.setHeight) {
      setValue(layer, "height", layer.height, layer.heightUnit, true, true);
    } else {
      style.clearHeight();
    }

    style = layer.child.getStyle();
    switch (layer.hPos) {
      case BEGIN:
        style.setLeft(0, Unit.PX);
        style.clearRight();
        break;
      case END:
        style.clearLeft();
        style.setRight(0, Unit.PX);
        break;
      case STRETCH:
        style.setLeft(0, Unit.PX);
        style.setRight(0, Unit.PX);
        break;
    }

    switch (layer.vPos) {
      case BEGIN:
        style.setTop(0, Unit.PX);
        style.clearBottom();
        break;
      case END:
        style.clearTop();
        style.setBottom(0, Unit.PX);
        break;
      case STRETCH:
        style.setTop(0, Unit.PX);
        style.setBottom(0, Unit.PX);
        break;
    }
  }

  private void setValue(Layer layer, String prop, double value, Unit unit,
      boolean vertical, boolean noNegative) {
    switch (unit) {
      case PX:
      case PCT:
        // Leave PX and PCT alone. PX doesn't need to be translated, and PCT
        // can't be.
        break;

      default:
        value = value
            * (int) getUnitSizeInPixels(layer.container, unit, vertical);
        unit = Unit.PX;
        break;
    }

    if (noNegative) {
      if (value < 0) {
        value = 0;
      }
    }

    layer.getContainerElement().getStyle().setProperty(prop, value, unit);
  }
}
