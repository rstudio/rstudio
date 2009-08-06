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
import com.google.gwt.user.client.ui.DisclosurePanel;

/**
 * Parses {@link com.google.gwt.user.client.ui.DisclosurePanel} widgets.
 */
public class DisclosurePanelParser implements ElementParser {

  private static final String ATTRIBUTE_HEADER_WIDGET = "DisclosurePanel-header";

  private static final String ATTRIBUTE_HEADER_BUNDLE = "imageBundle";

  private static final String ATTRIBUTE_INITIALLY_OPEN = "initiallyOpen";

  private static final String ATTRIBUTE_ENABLE_ANIMATION = "enableAnimation";

  /**
   * @return the type oracle's DisclosurePanel class
   */
  private static JClassType getDisclosurePanelClass(UiBinderWriter w) {
    return w.getOracle().findType(DisclosurePanel.class.getName());
  }

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    String text = null;
    // They must specify a label
    if (elem.hasAttribute("text")) {
      text = elem.consumeAttribute("text");
      text = '"' + UiBinderWriter.escapeTextForJavaStringLiteral(text) + '"';
    }

    // They may specify an image bundle
    String imageBundle = null;
    if (elem.hasAttribute(ATTRIBUTE_HEADER_BUNDLE)) {
      imageBundle = elem.consumeAttribute(ATTRIBUTE_HEADER_BUNDLE);
    }

    // They may specify an initial closed state
    String initiallyOpen = "false";
    if (elem.hasAttribute(ATTRIBUTE_INITIALLY_OPEN)) {
      initiallyOpen = elem.consumeAttribute(ATTRIBUTE_INITIALLY_OPEN);
    }

    // They may enable animation
    String enableAnimation = "true";
    if (elem.hasAttribute(ATTRIBUTE_ENABLE_ANIMATION)) {
      enableAnimation = elem.consumeAttribute(ATTRIBUTE_ENABLE_ANIMATION);
    }

    String childFieldName = null;
    String headerFieldName = null;

    for (XMLElement child : elem.consumeChildElements()) {
      // Disclosure panel header optionally comes from the DisclosurePanel-header attribute of the
      // child
      boolean childIsHeader = false;
      String headerAttributeName = elem.getPrefix() + ":" + ATTRIBUTE_HEADER_WIDGET;
      if (child.hasAttribute(headerAttributeName)) {
        if (headerFieldName != null) {
          writer.die("In %s, DisclosurePanel cannot contain more than one header widget.", elem);
        }
        child.consumeAttribute(headerAttributeName);
        headerFieldName = writer.parseWidget(child);
        childIsHeader = true;
      }
      if (!childIsHeader) {
        if (childFieldName != null) {
          writer.die("In %s, DisclosurePanel cannot contain more than one content widget.", elem);
        }
        childFieldName = writer.parseWidget(child);
      }
    }

    // To use the image bundle, you must provide a text header.
    if (imageBundle != null) {
      writer.setFieldInitializerAsConstructor(fieldName,
          getDisclosurePanelClass(writer), imageBundle, (text != null ? text : "\"\""),
          initiallyOpen);
    } else {
      JClassType panelClass = getDisclosurePanelClass(writer);
      if (text != null) {
        writer.setFieldInitializerAsConstructor(fieldName, panelClass, text);
      } else {
        writer.setFieldInitializerAsConstructor(fieldName, panelClass);
      }
    }
    if (childFieldName != null) {
      writer.addStatement("%1$s.setContent(%2$s);", fieldName, childFieldName);
    }
    if (headerFieldName != null) {
      writer.addStatement("%1$s.setHeader(%2$s);", fieldName, headerFieldName);
    }
    writer.addStatement("%1$s.setAnimationEnabled(%2$s);", fieldName, enableAnimation);
    writer.addStatement("%1$s.setOpen(%2$s);", fieldName, initiallyOpen);
  }
}
