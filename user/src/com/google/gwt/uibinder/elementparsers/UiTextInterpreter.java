/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLAttribute;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Interprets generic message tags like:
 * <b>&lt;ui:text from="{myMsg.message}" /&gt;</b>. It's called in both text
 * and HTML contexts.
 */
public class UiTextInterpreter implements XMLElement.Interpreter<String> {
  /**
   * Used in {@link #interpretElement} to invoke the {@link ComputedAttributeInterpreter}.
   */
  protected class Delegate implements ComputedAttributeInterpreter.Delegate {
    public String getAttributeToken(XMLAttribute attribute) throws UnableToCompleteException {
      return writer.tokenForStringExpression(attribute.getElement(), attribute.consumeStringValue());
    }
  }
  
  protected final UiBinderWriter writer;
  protected final ComputedAttributeInterpreter computedAttributeInterpreter;
  private final MortalLogger logger;

  public UiTextInterpreter(UiBinderWriter writer) {
    this.writer = writer;
    this.logger = writer.getLogger();
    this.computedAttributeInterpreter = createComputedAttributeInterpreter();
  }
  
  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
   // Must be in the format: <ui:string from="{myMsg.message}" />
   if (writer.isBinderElement(elem) && getLocalName().equals(elem.getLocalName())) {
     if (!elem.hasAttribute("from")) {
       logger.die(elem, "Attribute 'from' not found.");
     }
     if (!elem.getAttribute("from").hasComputedValue()) {
       logger.die(elem, "Attribute 'from' does not have a computed value");
     }
     // Make sure all computed attributes are interpreted first
     computedAttributeInterpreter.interpretElement(elem);
     
     String fieldRef = elem.consumeStringAttribute("from");
     // Make sure that "from" was the only attribute
     elem.assertNoAttributes();
     return "\" + " + fieldRef + " + \"";
   }
   return null;
  }
  
  protected ComputedAttributeInterpreter createComputedAttributeInterpreter() {
    return new ComputedAttributeInterpreter(writer);
  }
  
  protected String getLocalName() {
    return "text";
  }
}
