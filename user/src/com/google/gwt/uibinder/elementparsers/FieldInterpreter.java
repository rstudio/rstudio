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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Generates fields to hold named dom elements (e.g. &lt;div
 * ui:field="importantDiv"&gt;)
 */
 class FieldInterpreter implements XMLElement.Interpreter<String> {

  private final String element;
  private final UiBinderWriter writer;

  public FieldInterpreter(UiBinderWriter writer, String ancestorExpression) {
    this.writer = writer;
    this.element = ancestorExpression;
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    String fieldName = writer.declareFieldIfNeeded(elem);
    if (fieldName != null) {
      String token = writer.declareDomField(elem, fieldName, element);

      if (elem.hasAttribute("id")) {
        writer.die(elem, String.format(
            "Cannot declare id=\"%s\" and %s=\"%s\" on the same element",
            elem.consumeRawAttribute("id"), writer.getUiFieldAttributeName(),
            fieldName));
      }

      elem.setAttribute("id", token);
    }

    /*
     * Return null because we don't want to replace the dom element with any
     * particular string (though we may have consumed its id or gwt:field)
     */
    return null;
  }
}
