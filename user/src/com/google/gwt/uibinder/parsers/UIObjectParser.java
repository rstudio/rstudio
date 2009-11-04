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
 * Parser of all UIObject types. Basically bats cleanup if more specialized
 * parsers have left things around, or haven't been run.
 */
public class UIObjectParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    String debugId = elem.consumeStringAttribute("debugId", null);
    if (null != debugId) {
      writer.addStatement("%s.ensureDebugId(%s);", fieldName, debugId);
    }

    String[] styleNames = elem.consumeStringArrayAttribute("addStyleNames");
    for (String s : styleNames) {
      writer.addStatement("%s.addStyleName(%s);", fieldName, s);
    }

    styleNames = elem.consumeStringArrayAttribute("addStyleDependentNames");
    for (String s : styleNames) {
      writer.addStatement("%s.addStyleDependentName(%s);", fieldName, s);
    }

    HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
        writer, fieldName);

    String html = elem.consumeInnerHtml(interpreter);
    if (html.trim().length() > 0) {
      writer.setFieldInitializer(fieldName, String.format(
          "new DomHolder(UiBinderUtil.fromHtml(\"%s\"))", html));
    }
  }
}
