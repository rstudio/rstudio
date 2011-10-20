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
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

/**
 * A parser for menu items.
 */
public class MenuItemParser implements ElementParser {

  public void parse(final XMLElement elem, String fieldName, JClassType type,
      final UiBinderWriter writer) throws UnableToCompleteException {

    // Use special initializer for standard MenuItem,
    // custom subclass should have default constructor.
    if (MenuItem.class.getName().equals(type.getQualifiedSourceName())) {
      writer.setFieldInitializerAsConstructor(fieldName, "\"\"",
          "(com.google.gwt.user.client.Command) null");
    }

    final JClassType menuBarType = writer.getOracle().findType(
        MenuBar.class.getCanonicalName());

    class MenuBarInterpreter implements Interpreter<Boolean> {
      FieldWriter menuBarField = null;

      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {

        if (isMenuBar(child)) {
          if (menuBarField != null) {
            writer.die(child, "Only one MenuBar may be contained in a MenuItem");
          }
          menuBarField = writer.parseElementToField(child);
          return true;
        }

        return false;
      }

      boolean isMenuBar(XMLElement child) throws UnableToCompleteException {
        return menuBarType.isAssignableFrom(writer.findFieldType(child));
      }
    }

    MenuBarInterpreter interpreter = new MenuBarInterpreter();
    elem.consumeChildElements(interpreter);

    if (interpreter.menuBarField != null) {
      writer.genPropertySet(fieldName, "subMenu",
          interpreter.menuBarField.getNextReference());
    }
  }
}
