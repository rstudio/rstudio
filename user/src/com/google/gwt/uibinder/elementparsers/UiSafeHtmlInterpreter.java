/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.uibinder.rebind.XMLAttribute;

/**
 * Interprets generic message tags like:
 * <b>&lt;ui:safehtml from="{myMsg.message}" /&gt;</b>. It's called in HTML contexts.
 */
public class UiSafeHtmlInterpreter extends UiTextInterpreter {
  /**
   * Used in {@link #interpretElement} to invoke the {@link ComputedAttributeInterpreter}.
   */
  private class Delegate extends UiTextInterpreter.Delegate {
    public String getAttributeToken(XMLAttribute attribute) throws UnableToCompleteException {
      return writer.tokenForSafeHtmlExpression(attribute.getElement(),
          attribute.consumeSafeHtmlValue());
    }
  }

  public UiSafeHtmlInterpreter(UiBinderWriter writer) {
    super(writer);
  }
  
  protected ComputedAttributeInterpreter createComputedAttributeInterpreter() {
    return new ComputedAttributeInterpreter(writer, new Delegate());
  }
  
  @Override
  protected String getLocalName() {
    return "safehtml";
  }
}
