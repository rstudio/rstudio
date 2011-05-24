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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses a dom element and all of its children. Note that this parser does not
 * make recursive calls to parse child elements, unlike what goes on with widget
 * parsers. Instead, we consume the inner html of the given element into a
 * single string literal, used to instantiate the dom tree at run time.
 */
public class DomElementParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    HtmlInterpreter interpreter = new HtmlInterpreter(writer, fieldName,
        new HtmlMessageInterpreter(writer, fieldName));

    interpreter.interpretElement(elem);

    writer.beginAttachedSection(fieldName);
    String html = elem.consumeOpeningTag() + elem.consumeInnerHtml(interpreter)
        + elem.getClosingTag();
    writer.endAttachedSection();

    writer.setFieldInitializer(fieldName, String.format(
        "(%1$s) UiBinderUtil.fromHtml(%2$s)",
        type.getQualifiedSourceName(), writer.declareTemplateCall(html, fieldName)));
  }
}
