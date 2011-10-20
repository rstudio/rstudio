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
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;

/**
 * Parses {@link com.google.gwt.user.client.ui.MenuBar} widgets.
 */
public class MenuBarParser implements ElementParser {

  static final String BAD_CHILD = "Only MenuItem or MenuItemSeparator subclasses are valid children";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Generate instantiation (Vertical MenuBars require a ctor param).
    if (MenuBar.class.getName().equals(type.getQualifiedSourceName())) {
      if (elem.hasAttribute("vertical")) {
        String vertical = elem.consumeBooleanAttribute("vertical");
        writer.setFieldInitializerAsConstructor(fieldName, vertical);
      }
    }

    // Prepare base types.
    JClassType itemType = writer.getOracle().findType(MenuItem.class.getName());
    JClassType separatorType = writer.getOracle().findType(
        MenuItemSeparator.class.getName());

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      JClassType childType = writer.findFieldType(child);

      // MenuItem+
      if (itemType.isAssignableFrom(childType)) {
        FieldWriter childField = writer.parseElementToField(child);
        writer.addStatement("%1$s.addItem(%2$s);", fieldName, childField.getNextReference());
        continue;
      }

      // MenuItemSeparator+
      if (separatorType.isAssignableFrom(childType)) {
        FieldWriter childField = writer.parseElementToField(child);
        writer.addStatement("%1$s.addSeparator(%2$s);", fieldName, childField.getNextReference());
        continue;
      }

      // Fail
      writer.die(child, BAD_CHILD);
    }
  }
}
