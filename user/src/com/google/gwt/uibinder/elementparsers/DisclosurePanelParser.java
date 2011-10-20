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
 * Parses {@link com.google.gwt.user.client.ui.DisclosurePanel} widgets.
 */
public class DisclosurePanelParser implements ElementParser {

  private static class Children {
    XMLElement body;
    XMLElement header;
    XMLElement customHeader;
  }

  private static final String CUSTOM = "customHeader";
  private static final String HEADER = "header";
  private static final String OPEN_IMAGE = "openImage";
  private static final String CLOSED_IMAGE = "closedImage";

  public void parse(final XMLElement panelElem, String panelField,
      JClassType type, final UiBinderWriter writer)
      throws UnableToCompleteException {
    Children children = findChildren(panelElem, writer);

    if (children.body != null) {
      if (!writer.isWidgetElement(children.body)) {
        writer.die(children.body, "Must be a widget");
      }

      FieldWriter bodyField = writer.parseElementToField(children.body);
      writer.addStatement("%s.add(%s);", panelField, bodyField.getNextReference());
    }

    if (children.customHeader != null) {
      XMLElement headerElement = children.customHeader.consumeSingleChildElement();

      if (!writer.isWidgetElement(headerElement)) {
        writer.die(headerElement, "Must be a widget");
      }

      FieldWriter headerField = writer.parseElementToField(headerElement);
      writer.addStatement("%s.setHeader(%s);", panelField, headerField.getNextReference());
    }

    if (children.header != null) {
      String openImage = children.header.consumeImageResourceAttribute(OPEN_IMAGE);
      String closedImage = children.header.consumeImageResourceAttribute(CLOSED_IMAGE);
      String headerText = children.header.consumeInnerTextEscapedAsHtmlStringLiteral(new TextInterpreter(
          writer));

      if (openImage == null ^ closedImage == null) {
        writer.die(children.header,
            "Both %s and %s must be specified, or neither", OPEN_IMAGE,
            CLOSED_IMAGE);
      }

      String panelTypeName = type.getQualifiedSourceName();
      if (openImage != null) {
        writer.setFieldInitializer(panelField, String.format(
            "new %s(%s, %s, \"%s\")", panelTypeName, openImage, closedImage,
            headerText));
      } else {
        writer.setFieldInitializer(panelField,
            String.format("new %s(\"%s\")", panelTypeName, headerText));
      }
    }
  }

  private Children findChildren(final XMLElement elem,
      final UiBinderWriter writer) throws UnableToCompleteException {
    final Children children = new Children();

    elem.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {

        if (hasTag(child, HEADER)) {
          assertFirstHeader();
          children.header = child;
          return true;
        }

        if (hasTag(child, CUSTOM)) {
          assertFirstHeader();
          children.customHeader = child;
          return true;
        }

        // Must be the body, then
        if (children.body != null) {
          writer.die(elem, "May have only one body element");
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

      private boolean hasTag(XMLElement child, String tag) {
        return elem.getNamespaceUri().equals(child.getNamespaceUri())
            && tag.equals(child.getLocalName());
      }
    });

    return children;
  }
}
