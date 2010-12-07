/*
 * NodeTokenSource.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.codemirror;

import com.google.gwt.dom.client.Node;
import org.rstudio.studio.client.workbench.views.console.shell.BraceMatcher;

class NodeTokenSource implements BraceMatcher.TokenSource<Node>
{
   public NodeTokenSource(Node current)
   {
      current_ = current;
   }

   public Node next()
   {
      return current_ = next(current_, true);
   }

   private Node next(Node node, boolean allowChildren)
   {
      if (node == null)
         return null;
      Node result = allowChildren ? node.getFirstChild() : null;
      if (result != null)
         return result;
      result = node.getNextSibling();
      if (result != null)
         return result;
      return next(node.getParentNode(), false);
   }

   public Node prev()
   {
      if (current_ == null)
         return null;
      Node result = current_.getPreviousSibling();
      if (result == null)
         result = current_.getParentNode();

      current_ = result;
      return current_;
   }

   public Node currentToken()
   {
      return current_;
   }

   private Node current_;
}