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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * This implementation is used on IE8. Unlike {@code LayoutImpl}, it converts
 * all values to pixels before setting them. This is necessary because this
 * browser incorrectly calculates the relative sizes and positions of CSS
 * properties specified in certain units (e.g., when the value of an 'em' is
 * non-integral in pixels).
 */
public class LayoutImplIE8 extends LayoutImpl {

  private static native Layer getLayer(Element container) /*-{
    return container.__layer;
  }-*/;

  private static native void setLayer(Element container, Layer layer) /*-{
    // Potential leak: This is cleaned up in detach().
    container.__layer = layer;
  }-*/;

  @Override
  public void layout(Layer layer) {
    Style style = layer.container.getStyle();
    setLayer(layer.container, layer);

    // Whenever the visibility of a layer changes, we need to ensure that
    // layout() is run again for it. This is because the translation of units
    // to pixel values will be incorrect for invisible elements, and thus must
    // be fixed when they become visible.
    if (layer.visible) {
      String oldDisplay = style.getDisplay();
      style.clearDisplay();

      // We control the layer element, so assume that any non-zero display
      // property means it was set to 'none'.
      if (oldDisplay.length() > 0) {
        updateVisibility(layer.container);
      }
    } else {
      style.setDisplay(Display.NONE);
    }

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

  @Override
  public void onDetach(Element parent) {
    removeLayerRefs(parent);
  }

  private native void removeLayerRefs(Element parent) /*-{
    for (var i = 0; i < parent.childNodes.length; ++i) {
      var container = parent.childNodes[i];
      if (container.__layer) {
        container.__layer = null;
      }
    }
  }-*/;

  private void setValue(Layer layer, String prop, double value, Unit unit,
      boolean vertical, boolean noNegative) {
    switch (unit) {
      case PX:
      case PCT:
        // Leave PX and PCT alone. PX doesn't need to be translated, and PCT
        // can't be.
        break;

      default:
        value = value * getUnitSizeInPixels(layer.container, unit, vertical);
        value = (int) (value + 0.5); // Round to the nearest pixel.
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

  private void updateVisibility(Element container) {
    // If this element has an associated layer, re-run layout for it.
    Layer layer = getLayer(container);
    if (layer != null) {
      layout(layer);
    }

    // Walk all children, looking for elements with a '__layer' property. If one
    // exists, call layout() for that element. This is not cheap, but it's the
    // only way to correctly ensure that layout units get translated correctly.
    NodeList<Node> nodes = container.getChildNodes();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node node = nodes.getItem(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        updateVisibility(node.<Element>cast());
      }
    }
  }
}
