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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses {@link DockLayoutPanel} widgets.
 * 
 * TODO(jgw): The code that explicitly excludes SplitLayoutPanel in a fairly
 * awkward way could be greatly simplified if we hoisted the "dock-ness" into an
 * interface implemented by both DockLayoutPanel and SplitLayoutPanel, and moved
 * most of this code into a parser for that specific interface. This parser
 * would then be reduced to a simple special case for the ctor param.
 */
public class DockLayoutPanelParser implements ElementParser {

  private static final Map<String, String> DOCK_NAMES = new HashMap<String, String>();

  static {
    DOCK_NAMES.put("north", "addNorth");
    DOCK_NAMES.put("south", "addSouth");
    DOCK_NAMES.put("east", "addEast");
    DOCK_NAMES.put("west", "addWest");
    DOCK_NAMES.put("center", "add");
  }

  /**
   * TODO(jgw): This will be moved into EnumAttributeParser as soon as I get
   * around to building it.
   */
  static String getFullyQualifiedEnumName(Enum<?> e) {
    Class<?> cls = e.getClass();
    String clsName = cls.getCanonicalName();
    if (clsName == null) {
      // A synthesized enum subtype (e.g., Unit$3) will have no canonical name
      // (yet will not be marked as synthetic). Its superclass will be the
      // one we want (e.g., Unit).
      clsName = cls.getSuperclass().getCanonicalName();
    }
    return clsName + "." + e.name();
  }

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Generate instantiation (requires a 'unit' ctor param).
    // (Don't generate a ctor for the SplitLayoutPanel; it's implicitly PX).
    if (type != getSplitLayoutPanelType(writer)) {
      Unit unit = elem.consumeEnumAttribute("unit", Unit.class);
      writer.setFieldInitializerAsConstructor(fieldName,
          writer.getOracle().findType(DockLayoutPanel.class.getName()),
          getFullyQualifiedEnumName(unit));
    }

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // Make sure the element is one of the fixed set of valid directions.
      if (!isValidChildElement(elem, child)) {
        writer.die(
            "In %s, child must be one of {north, south, east, west, center}",
            elem);
      }

      // Consume the single widget element.
      XMLElement widget = child.consumeSingleChildElement();
      String childFieldName = writer.parseElementToField(widget);

      if (requiresSize(child)) {
        double size = child.consumeDoubleAttribute("size");
        writer.addStatement("%s.%s(%s, %f);", fieldName, addMethodName(child),
            childFieldName, size);
      } else {
        writer.addStatement("%s.%s(%s);", fieldName, addMethodName(child),
            childFieldName);
      }
    }

    // Emit the layout() call.
    writer.addStatement("%s.layout();", fieldName);
  }

  private String addMethodName(XMLElement elem) {
    return DOCK_NAMES.get(elem.getLocalName());
  }

  private JClassType getSplitLayoutPanelType(UiBinderWriter writer) {
    try {
      return writer.getOracle().getType(SplitLayoutPanel.class.getName());
    } catch (NotFoundException e) {
      throw new RuntimeException("Unexpected exception", e);
    }
  }

  private boolean isValidChildElement(XMLElement parent, XMLElement child) {
    return child.getNamespaceUri().equals(parent.getNamespaceUri())
        && DOCK_NAMES.containsKey(child.getLocalName());
  }

  private boolean requiresSize(XMLElement elem) {
    return !elem.getLocalName().equals("center");
  }
}
