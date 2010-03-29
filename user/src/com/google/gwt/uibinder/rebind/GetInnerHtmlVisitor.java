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
import org.w3c.dom.Node;
import org.w3c.dom.Text;

class GetInnerHtmlVisitor extends GetEscapedInnerTextVisitor {

  /**
   * Recursively gathers an HTML representation of the children of the given
   * Element, and stuffs it into the given StringBuffer. Applies the interpreter to
   * each descendant, and uses the writer to report errors.
   */
  public static void getEscapedInnerHtml(Element elem, StringBuffer buffer,
      Interpreter<String> interpreter, XMLElementProvider writer, DomCursor domCursor, 
      MortalLogger logger)
      throws UnableToCompleteException {   
    XMLElement xmlElement = writer.get(elem);
    domCursor.visitChild(xmlElement);
    new ChildWalker().accept(elem, new GetInnerHtmlVisitor(buffer, interpreter, writer, domCursor,
        logger));
    domCursor.finishChild(xmlElement);
  }
  
  private DomCursor domCursor;
  private final MortalLogger logger;

  private GetInnerHtmlVisitor(StringBuffer buffer, Interpreter<String> interpreter,
      XMLElementProvider writer, DomCursor domCursor, MortalLogger logger) {
    super(buffer, interpreter, writer);
    this.domCursor = domCursor;
    this.logger = logger;
  }
  
  @Override
  public void visitElement(Element elem) throws UnableToCompleteException {
    XMLElement xmlElement = elementProvider.get(elem);
    String replacement = interpreter.interpretElement(xmlElement);

    if (replacement != null) {
      buffer.append(replacement);
      return;
    }

    Node parent = elem.getParentNode();        
    buffer.append(xmlElement.consumeOpeningTag());    
    getEscapedInnerHtml(elem, buffer, interpreter, elementProvider, domCursor, logger);
    buffer.append(xmlElement.getClosingTag());       
    domCursor.advanceChild();
  }

  @Override
  public void visitText(Text t) {
    int startLength = buffer.length();
    super.visitText(t);
    if (buffer.length() != startLength && !buffer.toString().matches("^\\s*$")) {
      if (buffer.toString().substring(startLength).matches("^\\s*$")) {
        domCursor.advanceTextChild();
      }
    }
  }
}
