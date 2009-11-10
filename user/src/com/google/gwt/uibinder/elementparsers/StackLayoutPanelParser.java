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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.StackLayoutPanel;

/**
 * Parses {@link StackLayoutPanel} widgets.
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

    writer.setFieldInitializerAsConstructor(fieldName,
        writer.getOracle().findType(StackLayoutPanel.class.getName()),
        unit);

    // Parse children.
    for (XMLElement stackElem : panelElem.consumeChildElements()) {
      // Get the stack element.
      if (!isElementType(panelElem, stackElem, STACK)) {
        writer.die("In %s, only <%s:%s> children are allowed.", panelElem, 
            panelElem.getPrefix(), STACK);
      }
      
      // Find all the children of the <stack>.
      Children children = findChildren(stackElem, writer);

      // Parse the child widget.
      if (children.body == null) {
        writer.die("In %s, %s must have a child widget", panelElem, stackElem);
      }
      if (!writer.isWidgetElement(children.body)) {
        writer.die("In %s, %s must be a widget", stackElem, children.body);
      }
      String childFieldName = writer.parseElementToField(children.body);

      // Parse the header.
      if (children.header != null) {
        HtmlInterpreter htmlInt = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        String size = children.header.consumeDoubleAttribute("size");
        if ("".equals(size)) {
          writer.die("In %s, %s must have a size", panelElem, stackElem);
        }
        String html = children.header.consumeInnerHtml(htmlInt);
        writer.addStatement("%s.add(%s, " 
            + "new com.google.gwt.user.client.ui.HTML(\"%s\"), " 
            + "%s);", fieldName,
            childFieldName, html, size);
      } else if (children.customHeader != null) {
        XMLElement headerElement =
          children.customHeader.consumeSingleChildElement();
        String size = children.customHeader.consumeDoubleAttribute("size");
        if ("".equals(size)) {
          writer.die("In %s, %s must have a size", panelElem, stackElem);
        }
        if (!writer.isWidgetElement(headerElement)) {
          writer.die("In %s of %s, %s is not a widget", children.customHeader,
              stackElem, headerElement);
        }

        String headerField = writer.parseElementToField(headerElement);
        writer.addStatement("%s.add(%s, %s, %s);", fieldName, childFieldName,
            headerField, size);
      } else {
        // Neither a header or customHeader.
        writer.die("In %1$s, %2$s requires either a <%3$s:%4$s> or <%3$s:%5$s>",
            panelElem, stackElem, stackElem.getPrefix(), HEADER, CUSTOM);
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

    private boolean hasTag(XMLElement child, final String attribute) {
      return rightNamespace(child) && child.getLocalName().equals(attribute);
    }

    private boolean rightNamespace(XMLElement child) {
      return child.getNamespaceUri().equals(elem.getNamespaceUri());
    }
  });

  return children;
}

  private boolean isElementType(XMLElement parent, XMLElement child, String type) {
    return parent.getNamespaceUri().equals(child.getNamespaceUri())
        && type.equals(child.getLocalName());
  }
}
