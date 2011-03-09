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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.TabPanel} widgets.
 */
public class TabPanelParser implements ElementParser {

  private static final String TAG_TAB = "Tab";
  private static final String TAG_TABHTML = "TabHTML";

  public void parse(XMLElement panelElem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    writer.warn(panelElem,
        "%1$s:%2$s is deprecated. Use the %1$s:TabLayoutPanel instead.",
        panelElem.getPrefix(), panelElem.getLocalName());
    // Parse children.
    for (XMLElement tabElem : panelElem.consumeChildElements()) {
      // TabPanel can only contain Tab elements.
      if (!isElementType(panelElem, tabElem, TAG_TAB)) {
        writer.die(tabElem, "Only <%s:%s> children are allowed.",
            panelElem.getPrefix(), TAG_TAB);
      }

      // Get the caption, or null if there is none
      String tabCaption = tabElem.consumeStringAttribute("text");

      // Get the single required child widget.
      String tabHTML = null;
      String childFieldName = null;
      for (XMLElement tabChild : tabElem.consumeChildElements()) {
        if (tabChild.getLocalName().equals(TAG_TABHTML)) {
          if (tabCaption != null || tabHTML != null) {
            writer.die(tabElem,
                "May have only one \"text\" attribute or <%1$s:%2$s>",
                tabElem.getPrefix(), TAG_TABHTML);
          }
          HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
              writer, fieldName);
          tabHTML = tabChild.consumeInnerHtml(interpreter);
        } else {
          if (childFieldName != null) {
            writer.die(tabChild, "May only have a single child widget");
          }
          if (!writer.isWidgetElement(tabChild)) {
            writer.die(tabChild, "Must be a widget");
          }
          childFieldName = writer.parseElementToField(tabChild);
        }
      }

      if (childFieldName == null) {
        writer.die(tabElem, "Must have a child widget");
      }
      if (tabHTML != null) {
        writer.addStatement("%1$s.add(%2$s, %3$s, true);", fieldName,
            childFieldName, writer.declareTemplateCall(tabHTML));
      } else if (tabCaption != null) {
        writer.addStatement("%1$s.add(%2$s, %3$s);", fieldName, childFieldName,
            tabCaption);
      } else {
        writer.die(tabElem,
            "Requires either a \"text\" attribute or <%1$s:%2$s>",
            tabElem.getPrefix(), TAG_TABHTML);
      }
    }
  }

  private boolean isElementType(XMLElement parent, XMLElement child, String type) {
    return parent.getNamespaceUri().equals(child.getNamespaceUri())
        && type.equals(child.getLocalName());
  }
}
