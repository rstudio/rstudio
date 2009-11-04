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
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.StackLayoutPanel;

/**
 * Parses {@link StackLayoutPanel} widgets.
 */
public class StackLayoutPanelParser implements ElementParser {

  private static final String HEADER_ELEM = "header";
  private static final String STACK_ELEM = "stack";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    JEnumType unitEnumType = writer.getOracle().findType(
        Unit.class.getCanonicalName()).isEnum();
    String unit = elem.consumeAttribute("unit", unitEnumType);
    writer.setFieldInitializerAsConstructor(fieldName,
        writer.getOracle().findType(StackLayoutPanel.class.getName()),
        unit);

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // Get the stack element.
      if (!isElementType(elem, child, STACK_ELEM)) {
        writer.die("In %s, Only <stack> children are allowed.", elem);
      }

      XMLElement headerElem = null, widgetElem = null;
      for (XMLElement stackChild : child.consumeChildElements()) {
        // Get the header.
        if (isElementType(elem, stackChild, HEADER_ELEM)) {
          if (headerElem != null) {
            writer.die("In %s, Only one <header> allowed per <stack>", elem);
          }
          headerElem = stackChild;
          continue;
        }

        // Get the widget.
        if (widgetElem != null) {
          writer.die("In %s, Only one child widget allowed per <stack>", elem);
        }
        widgetElem = stackChild;
      }

      String size = headerElem.consumeDoubleAttribute("size");
      XMLElement headerWidgetElem = headerElem.consumeSingleChildElement();
      String headerFieldName = writer.parseElementToField(headerWidgetElem);
      String childFieldName = writer.parseElementToField(widgetElem);

      writer.addStatement("%s.add(%s, %s, %s);", fieldName, childFieldName,
          headerFieldName, size);
    }
  }

  private boolean isElementType(XMLElement parent, XMLElement child, String type) {
    return child.getNamespaceUri().equals(parent.getNamespaceUri())
        && type.equals(child.getLocalName());
  }
}
