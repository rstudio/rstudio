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
import com.google.gwt.uibinder.rebind.DomCursor;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * Parses {@link DialogBox} widgets.
 */
public class DialogBoxParser implements ElementParser {
  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    
    String caption = null;
    String body = null;

    for (XMLElement child : elem.consumeChildElements()) {
      if ("caption".equals(child.getLocalName())) {
        if (caption != null) {
          writer.die("In %s, may have only one <%s:caption>", elem,
              elem.getPrefix());
        }

        HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        DomCursor cursor = writer.beginDomSection(fieldName + ".getElement()");
        caption = child.consumeInnerHtml(interpreter, cursor);
        writer.endDomSection();
      } else {
        if (body != null) {
          writer.die("In %s, may have only one widget, but found %s and %s",
              elem, body, child);
        }
        if (!writer.isWidgetElement(child)) {
          writer.die("In %s, found non-widget %s", elem, child);
        }
        body = writer.parseElementToField(child);
      }
    }

    handleConstructorArgs(elem, fieldName, type, writer);
    
    if (caption != null) {
      writer.addStatement("%s.setHTML(\"%s\");", fieldName, caption);
    }
    if (body != null) {
      writer.addStatement("%s.setWidget(%s);", fieldName, body);
    }
  }

  /**
   * If this is DialogBox (not a subclass), parse constructor args
   * and generate the constructor call. For subtypes do nothing.
   */
  private void handleConstructorArgs(XMLElement elem, String fieldName,
      JClassType type, UiBinderWriter writer) throws UnableToCompleteException {
    boolean custom = !type.equals(writer.getOracle().findType(
        DialogBox.class.getCanonicalName()));
    if (!custom) {
      String autoHide = elem.consumeBooleanAttribute("autoHide", false);
      String modal = elem.consumeBooleanAttribute("modal", true);

      writer.setFieldInitializerAsConstructor(fieldName,
          writer.getOracle().findType(DialogBox.class.getCanonicalName()),
          autoHide, modal);
    }
  }
}
