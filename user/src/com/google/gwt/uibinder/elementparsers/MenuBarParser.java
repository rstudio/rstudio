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
import com.google.gwt.user.client.ui.MenuBar;

/**
 * Parses {@link com.google.gwt.user.client.ui.MenuBar} widgets.
 */
public class MenuBarParser implements ElementParser {
  private static final String TAG_MENUITEM = "MenuItem";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Generate instantiation (Vertical MenuBars require a ctor param).
    if (elem.hasAttribute("vertical")) {
      String vertical = elem.consumeBooleanAttribute("vertical");
      writer.setFieldInitializerAsConstructor(fieldName,
          writer.getOracle().findType(MenuBar.class.getName()), vertical);
    }

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // MenuBar can only contain MenuItem elements.
      {
        String ns = child.getNamespaceUri();
        String tagName = child.getLocalName();

        if (!elem.getNamespaceUri().equals(ns) || !tagName.equals(TAG_MENUITEM)) {
          writer.die("In %s, only <%s:%s> are valid children", elem,
              elem.getPrefix(), TAG_MENUITEM);
        }
      }

      String itemFieldName = writer.parseElementToField(child);

      writer.addStatement("%1$s.addItem(%2$s);", fieldName, itemFieldName);
    }
  }
}
