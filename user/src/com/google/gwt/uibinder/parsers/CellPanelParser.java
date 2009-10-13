/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.CellPanel} widgets.
 */
public class CellPanelParser implements ElementParser {

  private static final String HALIGN_ATTR = "horizontalAlignment";
  private static final String VALIGN_ATTR = "verticalAlignment";
  private static final String WIDTH_ATTR = "width";
  private static final String HEIGHT_ATTR = "height";
  private static final String CELL_TAG = "Cell";

  private static HorizontalAlignmentConstantParser halignParser =
      new HorizontalAlignmentConstantParser();
  private static VerticalAlignmentConstantParser valignParser =
      new VerticalAlignmentConstantParser();
  private static StringAttributeParser stringParser =
      new StringAttributeParser();

  /**
   * Parses the alignment and size attributes common to all CellPanels.
   *
   * This is exposed publicly because there is a DockPanelParser that overrides
   * the default behavior, but still needs to parse these attributes.
   *
   * @throws UnableToCompleteException
   */
  public static void parseCellAttributes(XMLElement cellElem, String fieldName,
      String childFieldName, UiBinderWriter writer) throws UnableToCompleteException {
    // Parse horizontal and vertical alignment attributes.
    if (cellElem.hasAttribute(HALIGN_ATTR)) {
      String value =
          halignParser.parse(cellElem.consumeAttribute(HALIGN_ATTR), writer);
      writer.addStatement("%1$s.setCellHorizontalAlignment(%2$s, %3$s);", fieldName,
          childFieldName, value);
    }

    if (cellElem.hasAttribute(VALIGN_ATTR)) {
      String value =
          valignParser.parse(cellElem.consumeAttribute(VALIGN_ATTR), writer);
      writer.addStatement("%1$s.setCellVerticalAlignment(%2$s, %3$s);", fieldName,
          childFieldName, value);
    }

    // Parse width and height attributes.
    if (cellElem.hasAttribute(WIDTH_ATTR)) {
      String value =
          stringParser.parse(cellElem.consumeAttribute(WIDTH_ATTR), writer);
      writer.addStatement("%1$s.setCellWidth(%2$s, %3$s);", fieldName, childFieldName,
          value);
    }

    if (cellElem.hasAttribute(HEIGHT_ATTR)) {
      String value =
          stringParser.parse(cellElem.consumeAttribute(HEIGHT_ATTR), writer);
      writer.addStatement("%1$s.setCellHeight(%2$s, %3$s);", fieldName, childFieldName,
          value);
    }
  }

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    for (XMLElement child : elem.consumeChildElements()) {
      String ns = child.getNamespaceUri();
      String tagName = child.getLocalName();
      if (ns.equals(elem.getNamespaceUri()) && tagName.equals(CELL_TAG)) {
        // It's a cell element, so parse its single child as a widget.
        XMLElement widget = child.consumeSingleChildElement();
        String childFieldName = writer.parseElementToField(widget);
        writer.addStatement("%1$s.add(%2$s);", fieldName, childFieldName);

        // Parse the cell tag's alignment & size attributes.
        parseCellAttributes(child, fieldName, childFieldName, writer);
      } else {
        // It's just a normal child, so parse it as a widget.
        String childFieldName = writer.parseElementToField(child);
        writer.addStatement("%1$s.add(%2$s);", fieldName, childFieldName);
      }
    }
  }
}
