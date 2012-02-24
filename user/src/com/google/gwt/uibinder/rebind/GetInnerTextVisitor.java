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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

class GetInnerTextVisitor implements NodeVisitor {

  /**
   * Gathers a text representation of the children of the given Elem, and stuffs
   * it into the given StringBuffer. Applies the interpreter to each descendant,
   * and uses the writer to report errors.
   */
  public static void getEscapedInnerText(Element elem, StringBuffer buffer,
      Interpreter<String> interpreter, XMLElementProvider writer)
      throws UnableToCompleteException {
    new ChildWalker().accept(elem, new GetInnerTextVisitor(buffer,
        interpreter, writer, false));
  }

  /**
   * Gathers a text representation of the children of the given Elem, and stuffs
   * it into the given StringBuffer. Applies the interpreter to each descendant,
   * and uses the writer to report errors. Escapes HTML Entities.
   */
  public static void getHtmlEscapedInnerText(Element elem, StringBuffer buffer,
      Interpreter<String> interpreter, XMLElementProvider writer)
      throws UnableToCompleteException {
    new ChildWalker().accept(elem, new GetInnerTextVisitor(buffer,
        interpreter, writer, true));
  }

  protected final StringBuffer buffer;
  protected final Interpreter<String> interpreter;
  protected final XMLElementProvider elementProvider;
  protected final boolean escapeHtmlEntities;

  protected GetInnerTextVisitor(StringBuffer buffer,
      Interpreter<String> interpreter, XMLElementProvider elementProvider) {
    this(buffer, interpreter, elementProvider, true);
  }

  protected GetInnerTextVisitor(StringBuffer buffer,
      Interpreter<String> interpreter, XMLElementProvider elementProvider,
      boolean escapeHtmlEntities) {
    this.buffer = buffer;
    this.interpreter = interpreter;
    this.elementProvider = elementProvider;
    this.escapeHtmlEntities = escapeHtmlEntities;
  }

  public void visitCData(CDATASection d) {
    // TODO(jgw): write this back just as it came in.
  }

  public void visitElement(Element e) throws UnableToCompleteException {
    String replacement = interpreter.interpretElement(elementProvider.get(e));

    if (replacement != null) {
      buffer.append(replacement);
    }
  }

  public void visitText(Text t) {
    String escaped;
    if (escapeHtmlEntities) {
      escaped = UiBinderWriter.escapeText(t.getTextContent(),
        preserveWhiteSpace(t));
    } else {
      escaped = t.getTextContent();
      if (!preserveWhiteSpace(t)) {
        escaped = escaped.replaceAll("\\s+", " ");
      }
      escaped = UiBinderWriter.escapeTextForJavaStringLiteral(escaped);
    }

    buffer.append(escaped);
  }

  private boolean preserveWhiteSpace(Text t) {
    Element parent = Node.ELEMENT_NODE == t.getParentNode().getNodeType()
        ? (Element) t.getParentNode() : null;

    boolean preserveWhitespace = parent != null
        && "pre".equals(parent.getTagName());
    // TODO(rjrjr) What about script blocks?
    return preserveWhitespace;
  }
}
