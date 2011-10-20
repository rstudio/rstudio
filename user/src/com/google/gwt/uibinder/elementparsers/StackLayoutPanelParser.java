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
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.StackLayoutPanel} widgets.
 */
public class StackLayoutPanelParser implements ElementParser {
  private static class Children {
    XMLElement body;
    XMLElement header;
    XMLElement customHeader;
  }

  private static final String CUSTOM = "customHeader";
  private static final String HEADER = "header";
  private static final String STACK = "stack";

  public void parse(XMLElement panelElem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    JEnumType unitEnumType = writer.getOracle().findType(
        Unit.class.getCanonicalName()).isEnum();
    String unit = panelElem.consumeAttributeWithDefault("unit",
        String.format("%s.%s", unitEnumType.getQualifiedSourceName(), "PX"),
        unitEnumType);

    writer.setFieldInitializerAsConstructor(fieldName, unit);

    // Parse children.
    for (XMLElement stackElem : panelElem.consumeChildElements()) {
      // Get the stack element.
      if (!isElementType(panelElem, stackElem, STACK)) {
        writer.die(stackElem, "Only <%s:%s> children are allowed.",
            panelElem.getPrefix(), STACK);
      }

      // Find all the children of the <stack>.
      Children children = findChildren(stackElem, writer);

      // Parse the child widget.
      if (children.body == null) {
        writer.die(stackElem, "Must have a child widget");
      }
      if (!writer.isWidgetElement(children.body)) {
        writer.die(children.body, "Must be a widget");
      }
      FieldWriter childField = writer.parseElementToField(children.body);

      // Parse the header.
      if (children.header != null) {
        HtmlInterpreter htmlInt = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        String size = children.header.consumeRequiredDoubleAttribute("size");
        String html = children.header.consumeInnerHtml(htmlInt);
        writer.addStatement("%s.add(%s, %s, true, %s);", fieldName,
            childField.getNextReference(), writer.declareTemplateCall(html, fieldName), size);
      } else if (children.customHeader != null) {
        XMLElement headerElement = children.customHeader.consumeSingleChildElement();
        String size = children.customHeader.consumeRequiredDoubleAttribute("size");
        if (!writer.isWidgetElement(headerElement)) {
          writer.die(headerElement, "Is not a widget");
        }

        FieldWriter headerField = writer.parseElementToField(headerElement);
        writer.addStatement("%s.add(%s, %s, %s);", fieldName, childField.getNextReference(),
            headerField.getNextReference(), size);
      } else {
        // Neither a header or customHeader.
        writer.die(stackElem, "Requires either a <%1$s:%2$s> or <%1$s:%3$s>",
            stackElem.getPrefix(), HEADER, CUSTOM);
      }
    }
  }

  private Children findChildren(final XMLElement elem,
      final UiBinderWriter writer) throws UnableToCompleteException {
    final Children children = new Children();

    elem.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {

        if (isElementType(elem, child, HEADER)) {
          assertFirstHeader();
          children.header = child;
          return true;
        }

        if (isElementType(elem, child, CUSTOM)) {
          assertFirstHeader();
          children.customHeader = child;
          return true;
        }

        // Must be the body, then
        if (children.body != null) {
          writer.die(children.body, "May have only one body element");
        }

        children.body = child;
        return true;
      }

      void assertFirstHeader() throws UnableToCompleteException {
        if (children.header != null || children.customHeader != null) {
          writer.die(elem, "May have only one <%1$s:header> "
              + "or <%1$s:customHeader>", elem.getPrefix());
        }
      }
    });

    return children;
  }

  private boolean isElementType(XMLElement parent, XMLElement child, String type) {
    return parent.getNamespaceUri().equals(child.getNamespaceUri())
        && type.equals(child.getLocalName());
  }
}
