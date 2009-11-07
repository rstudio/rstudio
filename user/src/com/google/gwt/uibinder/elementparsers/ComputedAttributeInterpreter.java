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
import com.google.gwt.uibinder.attributeparsers.AttributeParser;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLAttribute;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Assigns computed values to element attributes, e.g. nasty old
 * resources:styleName="style.pretty" and nice new
 * styleName={resources.style.pretty}, which will become something like
 * myWidget.setStyleName(resources.style().pretty()) in the generated code.
 */
class ComputedAttributeInterpreter implements XMLElement.Interpreter<String> {

  private final UiBinderWriter writer;

  public ComputedAttributeInterpreter(UiBinderWriter writer) {
    this.writer = writer;
  }

  @SuppressWarnings("deprecation")
  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    Map<String, String> attNameToToken = new HashMap<String, String>();

    for (int i = elem.getAttributeCount() - 1; i >= 0; i--) {
      XMLAttribute att = elem.getAttribute(i);
      AttributeParser parser = writer.getBundleAttributeParser(att);

      if (parser != null) {
        // Legacy res:style='style.pretty'
        String parsedValue = parser.parse(att.consumeRawValue(),
            writer.getLogger());
        String attToken = writer.tokenForExpression(parsedValue);

        // Use localName so <div res:style='...'> becomes <div style='...'>
        attNameToToken.put(att.getLocalName(), attToken);
        continue;
      }

      if (att.hasComputedValue()) {
        String attToken = writer.tokenForExpression(att.consumeStringValue());
        attNameToToken.put(att.getName(), attToken);
      } else {
        /*
         * No computed value, but make sure that any {{ madness gets escaped.
         * TODO(rjrjr) Move this to XMLElement RSN
         */
        String n = att.getName();
        String v = att.consumeRawValue().replace("{{", "{");
        elem.setAttribute(n, v);
      }
    }

    for (Map.Entry<String, String> attr : attNameToToken.entrySet()) {
      elem.setAttribute(attr.getKey(), attr.getValue());
    }

    // Return null because we don't want to replace the dom element
    return null;
  }
}
