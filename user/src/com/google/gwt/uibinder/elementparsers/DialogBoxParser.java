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
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DialogBox.Caption;

import java.util.Set;

/**
 * Parses {@link DialogBox} widgets.
 */
public class DialogBoxParser implements ElementParser {

  private static final String CAPTION = "caption";
  private static final String CUSTOM_CAPTION = "customCaption";
  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    String caption = null;
    FieldWriter body = null;
    XMLElement customCaption = null;

    String prefix = elem.getPrefix();

    for (XMLElement child : elem.consumeChildElements()) {
      if (CAPTION.equals(child.getLocalName())) {
        if (caption != null) {
          writer.die(elem, "May have only one <%s:%s>", prefix,
              CAPTION);
        }

        HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        caption = child.consumeInnerHtml(interpreter);
      } else if (CUSTOM_CAPTION.equals(child.getLocalName())) {
        if (customCaption != null) {
          writer.die(elem, "May only have one <%s:%s>", prefix,
              CUSTOM_CAPTION);
        }
        customCaption = child.consumeSingleChildElement();

      } else {
        if (body != null) {
          writer.die(elem, "May have only one widget, but found %s and %s",
              body.getName(), child);
        }
        if (!writer.isWidgetElement(child)) {
          writer.die(elem, "Found non-widget %s", child);
        }
        body = writer.parseElementToField(child);
      }
    }

    if (caption != null && customCaption != null) {
      writer.die("Must choose between <%s:%s> or <%s:%s>", prefix, CAPTION,
          prefix, CUSTOM_CAPTION);
    }

    handleConstructorArgs(elem, fieldName, type, writer, customCaption);

    if (caption != null) {
      writer.addStatement("%s.setHTML(%s);", fieldName,
          writer.declareTemplateCall(caption, fieldName));
    }
    if (body != null) {
      writer.addStatement("%s.setWidget(%s);", fieldName, body.getNextReference());
    }
  }

  /**
   * Determines if the element implements Caption.
   *
   * @throws UnableToCompleteException
   */
  protected boolean isCaption(UiBinderWriter writer, XMLElement element)
      throws UnableToCompleteException {
    JClassType type = writer.findFieldType(element);

    Set<? extends JClassType> classes = type.getFlattenedSupertypeHierarchy();
    JClassType captionType = writer.getOracle().findType(
        Caption.class.getCanonicalName());
    return classes.contains(captionType);
  }

  /**
   * Checks to see if the widget extends DialogBox or is DialogBox proper.
   */
  protected boolean isCustomWidget(UiBinderWriter writer, JClassType type) {
    return !type.equals(writer.getOracle().findType(
        DialogBox.class.getCanonicalName()));
  }

  /**
   * If this is DialogBox (not a subclass), parse constructor args and generate
   * the constructor call. For subtypes do nothing.
   */
  void handleConstructorArgs(XMLElement elem, String fieldName,
      JClassType type, UiBinderWriter writer, XMLElement customCaption)
      throws UnableToCompleteException {
    boolean custom = isCustomWidget(writer, type);
    if (!custom) {
      String autoHide = elem.consumeBooleanAttribute("autoHide", false);
      String modal = elem.consumeBooleanAttribute("modal", true);

      if (customCaption != null) {
        if (!writer.isWidgetElement(customCaption)) {
          writer.die(customCaption, "<%s:%s> must be a widget",
              customCaption.getPrefix(), CUSTOM_CAPTION);
        }
        if (!isCaption(writer, customCaption)) {
          writer.die(customCaption, "<%s:%s> must implement %s",
              customCaption.getPrefix(), CUSTOM_CAPTION,
              Caption.class.getCanonicalName());
        }
        FieldWriter fieldElement = writer.parseElementToField(customCaption);

        writer.setFieldInitializerAsConstructor(fieldName,
            autoHide, modal, fieldElement.getNextReference());
      } else {
        writer.setFieldInitializerAsConstructor(fieldName, autoHide, modal);
      }
    }
  }
}
