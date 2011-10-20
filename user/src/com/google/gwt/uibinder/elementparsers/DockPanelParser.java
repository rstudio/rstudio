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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.HashMap;

/**
 * Parses {@link com.google.gwt.user.client.ui.DockPanel} widgets.
 */
public class DockPanelParser implements ElementParser {

  private static final String TAG_DOCK = "Dock";
  private static final HashMap<String, String> values =
      new HashMap<String, String>();

  static {
    values.put("NORTH", "com.google.gwt.user.client.ui.DockPanel.NORTH");
    values.put("SOUTH", "com.google.gwt.user.client.ui.DockPanel.SOUTH");
    values.put("EAST", "com.google.gwt.user.client.ui.DockPanel.EAST");
    values.put("WEST", "com.google.gwt.user.client.ui.DockPanel.WEST");
    values.put("CENTER", "com.google.gwt.user.client.ui.DockPanel.CENTER");
    values.put("LINE_START", "com.google.gwt.user.client.ui.DockPanel.LINE_START");
    values.put("LINE_END", "com.google.gwt.user.client.ui.DockPanel.LINE_END");
  }

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // DockPanel can only contain Dock elements.
      String ns = child.getNamespaceUri();
      String tagName = child.getLocalName();

      if (!ns.equals(elem.getNamespaceUri())) {
        writer.die(elem, "Invalid DockPanel child namespace: " + ns);
      }
      if (!tagName.equals(TAG_DOCK)) {
        writer.die(elem, "Invalid DockPanel child element: " + tagName);
      }

      // And they must specify a direction.
      if (!child.hasAttribute("direction")) {
        writer.die(elem, "Dock must specify the 'direction' attribute");
      }
      String value = child.consumeRawAttribute("direction");
      String translated = values.get(value);
      if (translated == null) {
        writer.die(elem, "Invalid value: dockDirection='" + value + "'");
      }

      // And they can only have a single child widget.
      XMLElement widget = child.consumeSingleChildElement();
      FieldWriter childField = writer.parseElementToField(widget);
      writer.addStatement("%1$s.add(%2$s, %3$s);", fieldName,
          childField.getNextReference(), translated);

      // Parse the CellPanel-specific attributes on the Dock element.
      CellPanelParser.parseCellAttributes(child, fieldName, childField, writer);
    }
  }
}
