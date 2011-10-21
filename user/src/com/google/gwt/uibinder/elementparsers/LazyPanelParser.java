/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Parses {@link com.google.gwt.user.client.ui.LazyPanel} widgets.
 */
public class LazyPanelParser implements ElementParser {

  private static final String INITIALIZER_FORMAT = "new %s() {\n"
      + "  protected %s createWidget() {\n"
      + "    return %s;\n"
      + "  }\n"
      + "}";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    if (writer.getOwnerClass().getUiField(fieldName).isProvided()) {
      return;
    }

    if (!writer.useLazyWidgetBuilders()) {
      writer.die("LazyPanel only works with UiBinder.useLazyWidgetBuilders enabled.");
    }

    XMLElement child = elem.consumeSingleChildElement();
    if (!writer.isWidgetElement(child)) {
      writer.die(child, "Expecting only widgets in %s", elem);
    }

    FieldWriter childField = writer.parseElementToField(child);

    String lazyPanelClassPath = LazyPanel.class.getName();
    String widgetClassPath = Widget.class.getName();

    String code = String.format(INITIALIZER_FORMAT, lazyPanelClassPath,
        widgetClassPath, childField.getNextReference());
    writer.setFieldInitializer(fieldName, code);
  }
}
