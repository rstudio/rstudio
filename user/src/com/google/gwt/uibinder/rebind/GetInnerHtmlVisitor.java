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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;

import org.w3c.dom.Element;

class GetInnerHtmlVisitor extends GetInnerTextVisitor {

  /**
   * Recursively gathers an HTML representation of the children of the given
   * Elem, and stuffs it into the given StringBuffer. Applies the interpreter to
   * each descendant, and uses the writer to report errors.
   */
  public static void getEscapedInnerHtml(Element elem, StringBuffer buffer,
      Interpreter<String> interpreter, UiBinderWriter writer)
      throws UnableToCompleteException {
    new ChildWalker().accept(elem, new GetInnerHtmlVisitor(buffer, interpreter,
        writer));
  }

  private GetInnerHtmlVisitor(StringBuffer buffer,
      Interpreter<String> interpreter, UiBinderWriter writer) {
    super(buffer, interpreter, writer);
  }

  @Override
  public void visitElement(Element elem) throws UnableToCompleteException {
    XMLElement xmlElement = new XMLElement(elem, writer);
    String replacement = interpreter.interpretElement(xmlElement);
    if (replacement != null) {
      buffer.append(replacement);
      return;
    }

    // TODO(jgw): Ditch the closing tag when there are no children.
    buffer.append(xmlElement.consumeOpeningTag());
    getEscapedInnerHtml(elem, buffer, interpreter, writer);
    buffer.append(xmlElement.getClosingTag());
  }
}
