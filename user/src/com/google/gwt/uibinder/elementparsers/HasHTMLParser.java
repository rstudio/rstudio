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
 * Parses widgets that implement {@link com.google.gwt.user.client.ui.HasHTML}.
 */
public class HasHTMLParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    HtmlInterpreter interpreter =
      HtmlInterpreter.newInterpreterForUiObject(writer, fieldName);
    writer.beginAttachedSection(fieldName + ".getElement()");
    String html = elem.consumeInnerHtml(interpreter);
    writer.endAttachedSection();
    // TODO(jgw): throw an error if there's a conflicting 'html' attribute.
    if (html.trim().length() > 0) {
      writer.genPropertySet(fieldName, "HTML", writer.declareTemplateCall(html,
          fieldName));
    }
  }
}
