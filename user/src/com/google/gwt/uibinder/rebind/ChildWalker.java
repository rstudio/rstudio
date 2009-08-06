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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Traverses the children of a {@link Node}.
 */
class ChildWalker {

  /**
   * Take a {@link NodeVisitor} and show it each child of the given {@link Node}
   * that is of a type relevant to our templates.
   * <p>
   * Note that this is not a recursive call, though the visitor itself may
   * choose to recurse
   *
   * @throws UnableToCompleteException
   */
  void accept(Node n, NodeVisitor v) throws UnableToCompleteException {

    NodeList childNodes = n.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      Node child = childNodes.item(i);

      switch (child.getNodeType()) {
        case Node.ELEMENT_NODE:
          v.visitElement((Element) child);
          break;

        case Node.TEXT_NODE:
          v.visitText((Text) child);
          break;

        case Node.COMMENT_NODE:
          // Ditch comment nodes.
          break;

        case Node.CDATA_SECTION_NODE:
          v.visitCData((CDATASection) child);
          break;

        case Node.ENTITY_NODE:
        case Node.ENTITY_REFERENCE_NODE:
        case Node.ATTRIBUTE_NODE:
        case Node.DOCUMENT_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.NOTATION_NODE:
        case Node.PROCESSING_INSTRUCTION_NODE:
        default: {
          // None of these are expected node types.
          throw new RuntimeException("Unexpected XML node");
        }
      }
    }
  }
}
