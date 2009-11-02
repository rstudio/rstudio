/*
 * Copyright 2008 Google Inc.
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

/**
 * Parser of all UIObject types. Basically bats cleanup if more
 * specialized parsers have left things around, or haven't been run.
 */
public class UIObjectParser implements ElementParser {

  private static final String ATTRIBUTE_DEBUG_ID = "debugId";
  private static final String ATTRIBUTE_ADD_STYLE_NAMES = "addStyleNames";
  private static final String ATTRIBUTE_ADD_STYLE_DEPENDENT_NAMES = "addStyleDependentNames";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    addCustomAttribute(elem, fieldName, writer, "ensureDebugId", ATTRIBUTE_DEBUG_ID, false);
    addCustomAttribute(elem, fieldName, writer, "addStyleName", ATTRIBUTE_ADD_STYLE_NAMES, true);
    addCustomAttribute(elem, fieldName, writer, "addStyleDependentName",
        ATTRIBUTE_ADD_STYLE_DEPENDENT_NAMES, true);

    HtmlInterpreter interpreter =
      HtmlInterpreter.newInterpreterForUiObject(writer, fieldName);

    String html = elem.consumeInnerHtml(interpreter);
    if (html.trim().length() > 0) {
      writer.setFieldInitializer(fieldName,
          String.format("new DomHolder(UiBinderUtil.fromHtml(\"%s\"))", html));
    }
  }

  /**
   * @param elem The actual XmlElement which the parser is inspecting
   * @param fieldName The object/type to which a custom attribute will be added
   * @param writer The TemplateWriter for generating the code
   * @param targetSetterMethod The real method/setter used by UIObject to set
   *        the attribute
   * @param attribute The attribute as it will appear in the binder template
   * @param isMultiValue Specifies if the attributes value can have multiple
   *        values that are comma separated
   * @throws UnableToCompleteException
   */
  private void addCustomAttribute(XMLElement elem, String fieldName, UiBinderWriter writer,
      String targetSetterMethod, String attribute, boolean isMultiValue)
      throws UnableToCompleteException {
    String attributeValue = null;
    if (elem.hasAttribute(attribute)) {
      attributeValue = elem.consumeRawAttribute(attribute);

      if ("".equals(attributeValue)) {
        writer.die("In %s, value for attribute %s cannot be empty", elem, attribute);
      }

      // Check if the value is comma separated
      if (isMultiValue && attributeValue.contains(",")) {
        String[] values = attributeValue.split(",");
        for (String value : values) {
          value = value.trim();
          if ("".equals(value)) {
            writer.die("In %s, value for attribute %s cannot be empty", elem, attribute);
          }
          writer.addStatement("%1$s.%2$s(\"%3$s\");", fieldName, targetSetterMethod, value);
        }
      } else {
        writer.addStatement("%1$s.%2$s(\"%3$s\");", fieldName, targetSetterMethod, attributeValue);
      }
    }
  }
}
