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

/**
 * Parses {@link com.google.gwt.user.client.ui.StackPanel} widgets.
 */
public class StackPanelParser implements ElementParser {

  private static final String ATTRIBUTE_TEXT = "StackPanel-text";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      if (!writer.isWidgetElement(child)) {
        writer.die(child, "Widget required");
      }
      
      // Stack panel label comes from the StackPanel-text attribute of the child
      String stackItemLabel = null;
      String variableAttributeName = elem.getPrefix() + ":" + ATTRIBUTE_TEXT;
      if (child.hasAttribute(variableAttributeName)) {
        stackItemLabel = child.consumeRawAttribute(variableAttributeName);
      }

      FieldWriter childField = writer.parseElementToField(child);
      if (stackItemLabel == null) {
        writer.addStatement("%1$s.add(%2$s);", fieldName,
            childField.getNextReference());
      } else {
        writer.addStatement("%1$s.add(%2$s, \"%3$s\");", fieldName,
            childField.getNextReference(), stackItemLabel);
      }
    }
  }
}
