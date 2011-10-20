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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.LayoutPanel LayoutPanel} widgets.
 */
public class LayoutPanelParser implements ElementParser {

  private static final String ERR_PAIRING = "'%s' must be paired with '%s' or '%s'.";
  private static final String ERR_TOO_MANY = "There are too many %s constraints.";
  private static final String ERR_NEGATIVE_WIDTH = "Attribute 'width' can not be negative.";
  private static final String ERR_NEGATIVE_HEIGHT = "Attribute 'height' can not be negative.";
  private static final String LAYER = "layer";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    // Parse children.
    for (XMLElement layerElem : elem.consumeChildElements()) {
      // Get the layer element.
      if (!isElementType(elem, layerElem, LAYER)) {
        writer.die(layerElem, "Only <%s:%s> children are allowed.",
            elem.getPrefix(), LAYER);
      }

      // Get the child widget element.
      FieldWriter childField = writer.parseElementToField(layerElem.consumeSingleChildElement());
      writer.addStatement("%1$s.add(%2$s);", fieldName, childField.getNextReference());

      // Parse the horizontal layout constraints.
      String left = layerElem.consumeLengthAttribute("left");
      String right = layerElem.consumeLengthAttribute("right");
      String width = layerElem.consumeLengthAttribute("width");
      if (isNegative(width)) {
        writer.die(layerElem, ERR_NEGATIVE_WIDTH);
      }

      if (left != null) {
        if (right != null) {
          if (width != null) {
            writer.die(layerElem, ERR_TOO_MANY, "horizontal");
          }
          generateConstraint(fieldName, childField.getNextReference(), "LeftRight", left,
              right, writer);
        } else if (width != null) {
          generateConstraint(fieldName, childField.getNextReference(), "LeftWidth", left,
              width, writer);
        } else {
          writer.die(layerElem, ERR_PAIRING, "left", "right", "width");
        }
      } else if (right != null) {
        if (width != null) {
          generateConstraint(fieldName, childField.getNextReference(), "RightWidth", right,
              width, writer);
        } else {
          writer.die(layerElem, ERR_PAIRING, "right", "left", "width");
        }
      }

      // Parse the vertical layout constraints.
      String top = layerElem.consumeLengthAttribute("top");
      String bottom = layerElem.consumeLengthAttribute("bottom");
      String height = layerElem.consumeLengthAttribute("height");
      if (isNegative(height)) {
        writer.die(layerElem, ERR_NEGATIVE_HEIGHT);
      }

      if (top != null) {
        if (bottom != null) {
          if (height != null) {
            writer.die(layerElem, ERR_TOO_MANY, "vertical");
          }
          generateConstraint(fieldName, childField.getNextReference(), "TopBottom", top,
              bottom, writer);
        } else if (height != null) {
          generateConstraint(fieldName, childField.getNextReference(), "TopHeight", top,
              height, writer);
        } else {
          writer.die(layerElem, ERR_PAIRING, "top", "bottom", "height");
        }
      } else if (bottom != null) {
        if (height != null) {
          generateConstraint(fieldName, childField.getNextReference(), "BottomHeight", bottom,
              height, writer);
        } else {
          writer.die(layerElem, ERR_PAIRING, "bottom", "top", "height");
        }
      }
    }
  }

  private void generateConstraint(String panelName, String widgetName,
      String constraintName, String first, String second, UiBinderWriter writer) {
    writer.addStatement("%s.setWidget%s(%s, %s, %s);", panelName,
        constraintName, widgetName, first, second);
  }

  private boolean isElementType(XMLElement parent, XMLElement child, String type) {
    return parent.getNamespaceUri().equals(child.getNamespaceUri())
        && type.equals(child.getLocalName());
  }

  /**
   * @return <code>true</code> if in given <code>value, unit</code> expression
   *         <code>value</code> part is obviously negative.
   */
  private boolean isNegative(String expression) {
    if (expression != null && expression.length() >= 2
        && expression.charAt(0) == '-') {
      char secondChar = expression.charAt(1);
      return Character.isDigit(secondChar);
    }
    return false;
  }
}
