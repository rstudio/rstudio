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
import com.google.gwt.uibinder.rebind.DomCursor;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.TabPanel} widgets.
 */
public class TabPanelParser implements ElementParser {

  private static final String TAG_TAB = "Tab";
  private static final String TAG_TABHTML = "TabHTML";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    writer.warn(
        "%1$s:%2$s is deprecated. Use the %1$s:TabLayoutPanel instead.",
        elem.getPrefix(), elem.getLocalName());
    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // TabPanel can only contain Tab elements.
      String ns = child.getNamespaceUri();
      String tagName = child.getLocalName();

      if (!ns.equals(elem.getNamespaceUri())) {
        writer.die("Invalid TabPanel child namespace: " + ns);
      }
      if (!tagName.equals(TAG_TAB)) {
        writer.die("Invalid TabPanel child element: " + tagName);
      }

      // Get the caption, if any.
      String tabCaption = "";
      if (child.hasAttribute("text")) {
        tabCaption = child.consumeRawAttribute("text");
      }

      // Get the single required child widget.
      String tabHTML = null;
      String childFieldName = null;
      for (XMLElement tabChild : child.consumeChildElements()) {
        if (tabChild.getLocalName().equals(TAG_TABHTML)) {
          HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
              writer, fieldName);
          DomCursor cursor = writer.beginDomSection(fieldName + ".getElement()");
          tabHTML = tabChild.consumeInnerHtml(interpreter, cursor);
          writer.endDomSection();
        } else {
          if (childFieldName != null) {
            writer.die("%s may only have a single child widget", child);
          }
          childFieldName = writer.parseElementToField(tabChild);
        }
      }

      if (childFieldName == null) {
        writer.die("%s must have a child widget", child);
      }

      if (tabHTML != null) {
        writer.addStatement("%1$s.add(%2$s, \"%3$s\", true);", fieldName,
            childFieldName, tabHTML);
      } else {
        writer.addStatement("%1$s.add(%2$s, \"%3$s\");", fieldName,
            childFieldName, tabCaption);
      }
    }
  }
}
