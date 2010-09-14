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

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.EX;
import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * Default implementation, which works with all browsers except for IE6. It uses
 * only the "top", "left", "bottom", "right", "width", and "height" CSS
 * properties.
 * 
 * Note: This implementation class has state, so {@link Layout} must create a
 * new instance for each layout-parent.
 */
class LayoutImpl {

  private static DivElement fixedRuler;

  static {
    fixedRuler = createRuler(Unit.CM, Unit.CM);
    Document.get().getBody().appendChild(fixedRuler);
  }

  protected static DivElement createRuler(Unit widthUnit, Unit heightUnit) {
    DivElement ruler = Document.get().createDivElement();
    ruler.setInnerHTML("&nbsp;");
    Style style = ruler.getStyle();
    style.setPosition(Position.ABSOLUTE);
    style.setZIndex(-32767);

    // Position the ruler off the top edge, double the size just to be
    // extra sure it doesn't show up on the screen.
    style.setTop(-20, heightUnit);

    // Note that we are making the ruler element 10x10, because some browsers
    // generate non-integral ratios (e.g., 1em == 13.3px), so we need a little
    // extra precision.
    style.setWidth(10, widthUnit);
    style.setHeight(10, heightUnit);
    return ruler;
  }

  protected DivElement relativeRuler;

  public Element attachChild(Element parent, Element child, Element before) {
    DivElement container = Document.get().createDivElement();
    container.appendChild(child);

    container.getStyle().setPosition(Position.ABSOLUTE);
    container.getStyle().setOverflow(Overflow.HIDDEN);

    fillParent(child);

    Element beforeContainer = null;
    if (before != null) {
      beforeContainer = before.getParentElement();
      assert beforeContainer.getParentElement()
          == parent : "Element to insert before must be a sibling";
    }
    parent.insertBefore(container, beforeContainer);
    return container;
  }

  public void fillParent(Element elem) {
    Style style = elem.getStyle();
    style.setPosition(Position.ABSOLUTE);
    style.setLeft(0, PX);
    style.setTop(0, PX);
    style.setRight(0, PX);
    style.setBottom(0, PX);
  }

  /**
   * @param parent the parent element
   */
  public void finalizeLayout(Element parent) {
  }

  public double getUnitSizeInPixels(
      Element parent, Unit unit, boolean vertical) {
    if (unit == null) {
      return 1;
    }

    switch (unit) {
      case PCT:
        return (vertical ? parent.getClientHeight() : parent.getClientWidth())
            / 100.0;
      case EM:
        return relativeRuler.getOffsetWidth() / 10.0;
      case EX:
        return relativeRuler.getOffsetHeight() / 10.0;
      case CM:
        return fixedRuler.getOffsetWidth() * 0.1; // 1.0 cm / cm
      case MM:
        return fixedRuler.getOffsetWidth() * 0.01; // 0.1 cm / mm
      case IN:
        return fixedRuler.getOffsetWidth() * 0.254; // 2.54 cm / in
      case PT:
        return fixedRuler.getOffsetWidth() * 0.00353; // 0.0353 cm / pt
      case PC:
        return fixedRuler.getOffsetWidth() * 0.0423; // 0.423 cm / pc
      default:
      case PX:
        return 1;
    }
  }

  public void initParent(Element parent) {
    parent.getStyle().setPosition(Position.RELATIVE);
    parent.appendChild(relativeRuler = createRuler(EM, EX));
  }

  public void layout(Layer layer) {
    Style style = layer.container.getStyle();

    if (layer.visible) {
      style.clearDisplay();
    } else {
      style.setDisplay(Display.NONE);
    }

    style.setProperty(
        "left", layer.setLeft ? (layer.left + layer.leftUnit.getType()) : "");
    style.setProperty(
        "top", layer.setTop ? (layer.top + layer.topUnit.getType()) : "");
    style.setProperty("right",
        layer.setRight ? (layer.right + layer.rightUnit.getType()) : "");
    style.setProperty("bottom",
        layer.setBottom ? (layer.bottom + layer.bottomUnit.getType()) : "");
    style.setProperty("width",
        layer.setWidth ? (layer.width + layer.widthUnit.getType()) : "");
    style.setProperty("height",
        layer.setHeight ? (layer.height + layer.heightUnit.getType()) : "");

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

  @SuppressWarnings("unused")
  public void onAttach(Element parent) {
    // Do nothing. This exists only to help LayoutImplIE6 avoid memory leaks.
  }

  @SuppressWarnings("unused")
  public void onDetach(Element parent) {
    // Do nothing. This exists only to help LayoutImplIE6 avoid memory leaks.
  }

  public void removeChild(Element container, Element child) {
    container.removeFromParent();

    // We want this code to be resilient to the child having already been
    // removed from its container (perhaps by widget code).
    if (child.getParentElement() == container) {
      child.removeFromParent();
    }

    // Cleanup child styles set by fillParent().
    Style style = child.getStyle();
    style.clearPosition();
    style.clearLeft();
    style.clearTop();
    style.clearWidth();
    style.clearHeight();
  }
}
