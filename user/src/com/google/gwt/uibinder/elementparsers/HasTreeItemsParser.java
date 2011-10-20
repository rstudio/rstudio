/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Parses {@link com.google.gwt.user.client.ui.Tree} widgets.
 */
public class HasTreeItemsParser implements ElementParser {

  static final String BAD_CHILD = "Only TreeItem or Widget subclasses are valid children";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Prepare base types.
    JClassType itemType = writer.getOracle().findType(TreeItem.class.getName());
    JClassType widgetType = writer.getOracle().findType(Widget.class.getName());
    JClassType isWidgetType = writer.getOracle().findType(IsWidget.class.getName());

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      JClassType childType = writer.findFieldType(child);

      // TreeItem+
      if (itemType.isAssignableFrom(childType)) {
        FieldWriter childField = writer.parseElementToField(child);
        writer.addStatement("%1$s.addItem(%2$s);", fieldName,
            childField.getNextReference());
        continue;
      }

      // Widget+ or IsWidget+
      if (widgetType.isAssignableFrom(childType)
          || isWidgetType.isAssignableFrom(childType)) {
        FieldWriter childField = writer.parseElementToField(child);
        writer.addStatement("%1$s.addItem(%2$s);", fieldName,
            childField.getNextReference());
        continue;
      }

      // Fail
      writer.die(child, BAD_CHILD);
    }
  }
}
