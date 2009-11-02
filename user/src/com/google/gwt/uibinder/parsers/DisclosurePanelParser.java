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

    if (null != children.body) {
      if (!writer.isWidgetElement(children.body)) {
        writer.die("In %s, %s must be a widget", panelElem, children.body);
      }

      String bodyField = writer.parseElementToField(children.body);
      writer.addInitStatement("%s.add(%s);", panelField, bodyField);
    }

    if (null != children.customHeader) {
      XMLElement headerElement = children.customHeader.consumeSingleChildElement();

      if (!writer.isWidgetElement(headerElement)) {
        writer.die("In %s of %s, %s is not a widget", children.customHeader,
            panelElem, headerElement);
      }

      String headerField = writer.parseElementToField(headerElement);
      writer.addInitStatement("%s.setHeader(%s);", panelField, headerField);
    }

    if (null != children.header) {
      String openImage = getAttribute(OPEN_IMAGE, children.header, writer);
      String closedImage = getAttribute(CLOSED_IMAGE, children.header, writer);
      String headerText = children.header.consumeInnerTextEscapedAsHtmlStringLiteral(new TextInterpreter(
          writer));

      if ((openImage == null || closedImage == null)
          && !(openImage == closedImage)) {
        writer.die("In %s of %s, both %s and %s must be specified, or neither",
            children.header, panelElem, OPEN_IMAGE, CLOSED_IMAGE);
      }

      String panelTypeName = type.getQualifiedSourceName();
      if (openImage != null) {
        writer.setFieldInitializer(panelField, String.format(
            "new %s(%s, %s, \"%s\")", panelTypeName, openImage, closedImage,
            headerText));
      } else {
        writer.setFieldInitializer(panelField, String.format("new %s(\"%s\")",
            panelTypeName, headerText));
      }
    }
  }

  private Children findChildren(final XMLElement elem,
      final UiBinderWriter writer) throws UnableToCompleteException {
    final Children children = new Children();

    elem.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {

        if (hasAttribute(child, HEADER)) {
          assertFirstHeader();
          children.header = child;
          return true;
        }

        if (hasAttribute(child, CUSTOM)) {
          assertFirstHeader();
          children.customHeader = child;
          return true;
        }

        // Must be the body, then
        if (null != children.body) {
          writer.die("In %s, may have only one body element", elem);
        }

        children.body = child;
        return true;
      }

      void assertFirstHeader() throws UnableToCompleteException {
        if ((null != children.header) && (null != children.customHeader)) {
          writer.die("In %1$s, may have only one %2$s:header "
              + "or %2$s:customHeader", elem, elem.getPrefix());
        }
      }

      private boolean hasAttribute(XMLElement child, final String attribute) {
        return rightNamespace(child) && child.getLocalName().equals(attribute);
      }

      private boolean rightNamespace(XMLElement child) {
        return child.getNamespaceUri().equals(elem.getNamespaceUri());
      }
    });

    return children;
  }

  /**
   * @return a field reference or a null string if the attribute is unset
   * @throws UnableToCompleteException on bad value
   */
  private String getAttribute(String attribute, XMLElement headerElem,
      UiBinderWriter writer) throws UnableToCompleteException {
    // TODO(rjrjr) parser should come from XMLElement

    String value = headerElem.consumeRawAttribute(attribute, null);
    if (value != null) {
      value = new StrictAttributeParser().parse(value, writer.getLogger());
    }
    return value;
  }
}
