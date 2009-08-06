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
 * Parses a raw dom element, as opposed to a widget or uiobject. Note that this
 * parser does not get a crack at every dom element--it only handles the case of
 * a dom element as ui root. Note also that it is intended to be used only by
 * {@link UiBinderWriter}, and will generate cast class exceptions if used
 * otherwise.
 */
public class DomElementParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    HtmlInterpreter interpreter =
        new HtmlInterpreter(writer, "root", new HtmlMessageInterpreter(writer,
            "root"));

    writer.setFieldInitializer(fieldName, "null");

    interpreter.interpretElement(elem);
    String rootHtml = elem.consumeOpeningTag() + elem.getClosingTag();
    String rootType = getFQTypeName(type);
    writer.genRoot(String.format(
        "(%1$s) UiBinderUtil.fromHtml(\"%2$s\")", rootType, rootHtml));

    String innerHtml = elem.consumeInnerHtml(interpreter);

    if (innerHtml.trim().length() > 0) { // TODO(rjrjr) Really want this check?
      writer.addStatement("root.setInnerHTML(\"%s\");", innerHtml);
    }
  }

  private String getFQTypeName(JClassType type) {
    return type.getPackage().getName() + "." + type.getName();
  }
}
