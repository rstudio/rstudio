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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link LayoutPanel} widgets.
 */
public class LayoutPanelParser implements ElementParser {

  private static final String ERR_PAIRING = "In %s %s, 'left' must be paired with 'right' or 'width'.";
  private static final String ERR_TOO_MANY = "In %s %s, there are too many %s constraints.";
  private static final String LAYER = "layer";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    // Parse children.
    for (XMLElement layerElem : elem.consumeChildElements()) {
      // Get the layer element.
      if (!isElementType(elem, layerElem, LAYER)) {
        writer.die("In %s, only <%s:%s> children are allowed.", elem,
            elem.getPrefix(), LAYER);
      }

      // Get the child widget element.
      String childFieldName = writer.parseElementToField(
          layerElem.consumeSingleChildElement());
      writer.addStatement("%1$s.add(%2$s);", fieldName, childFieldName);

      // Parse the horizontal layout constraints.
      String left = maybeConsumeLengthAttribute(layerElem, "left");
      String right = maybeConsumeLengthAttribute(layerElem, "right");
      String width = maybeConsumeLengthAttribute(layerElem, "width");

      if (left != null) {
        if (right != null) {
          if (width != null) {
            writer.die(ERR_TOO_MANY, elem, layerElem, "horizontal");
          }
          generateConstraint(fieldName, childFieldName, "LeftRight", left,
              right, writer);
        } else if (width != null) {
          generateConstraint(fieldName, childFieldName, "LeftWidth", left,
              width, writer);
        } else {
          writer.die(ERR_PAIRING, elem, layerElem, "left", "right", "width");
        }
      } else if (right != null) {
        if (width != null) {
          generateConstraint(fieldName, childFieldName, "RightWidth", right,
              width, writer);
        } else {
          writer.die(ERR_PAIRING, elem, layerElem, "right", "left", "width");
        }
      }

      // Parse the vertical layout constraints.
      String top = maybeConsumeLengthAttribute(layerElem, "top");
      String bottom = maybeConsumeLengthAttribute(layerElem, "bottom");
      String height = maybeConsumeLengthAttribute(layerElem, "height");

      if (top != null) {
        if (bottom != null) {
          if (height != null) {
            writer.die(ERR_TOO_MANY, elem, layerElem, "vertical");
          }
          generateConstraint(fieldName, childFieldName, "TopBottom", top,
              bottom, writer);
        } else if (height != null) {
          generateConstraint(fieldName, childFieldName, "TopHeight", top,
              height, writer);
        } else {
          writer.die(ERR_PAIRING, elem, layerElem, "top", "bottom", "height");
        }
      } else if (bottom != null) {
        if (height != null) {
          generateConstraint(fieldName, childFieldName, "BottomHeight", bottom,
              height, writer);
        } else {
          writer.die(ERR_PAIRING, elem, layerElem, "bottom", "top", "height");
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

  private String maybeConsumeLengthAttribute(XMLElement elem, String name)
      throws UnableToCompleteException {
    return elem.hasAttribute(name) ? elem.consumeLengthAttribute(name) : null;
  }
}
