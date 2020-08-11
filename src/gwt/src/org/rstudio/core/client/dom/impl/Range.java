/*
 * Range.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.dom.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

class Range extends JavaScriptObject
{
   protected Range()
   {
   }

   public static native Range create(Document doc) /*-{
      return doc.createRange();
   }-*/;

   public final native boolean isCollapsed() /*-{
      return this.collapsed;
   }-*/;

   public final native Node getCommonAncestorContainer() /*-{
      return this.commonAncestorContainer;
   }-*/;

   public final native Node getEndContainer() /*-{
      return this.endContainer;
   }-*/;

   public final native int getEndOffset() /*-{
      return this.endOffset;
   }-*/;

   public final native Node getStartContainer() /*-{
      return this.startContainer;
   }-*/;

   public final native int getStartOffset() /*-{
      return this.startOffset;
   }-*/;

   public final native JavaScriptObject cloneContents() /*-{
      return this.cloneContents();
   }-*/;

   public final native Range cloneRange() /*-{
      return this.cloneRange();
   }-*/;

   public final native void collapse(boolean toStart) /*-{
      return this.collapse(toStart);
   }-*/;

   public final native short compareBoundaryPoints(short how,
                                                   Range sourceRange) /*-{
      return this.compareBoundaryPoints(how, sourceRange);
   }-*/;

   public final native void deleteContents() /*-{
      return this.deleteContents();
   }-*/;

   public final native void detach() /*-{
      return this.detach();
   }-*/;

   public final native JavaScriptObject extractContents() /*-{
      return this.extractContents();
   }-*/;

   public final native void insertNode(Node newNode) /*-{
      return this.insertNode(newNode);
   }-*/;

   public final native void selectNode(Node refNode) /*-{
      return this.selectNode(refNode);
   }-*/;

   public final native void selectNodeContents(Node refNode) /*-{
      return this.selectNodeContents(refNode);
   }-*/;

   public final native void setEnd(Node refNode, int offset) /*-{
      return this.setEnd(refNode, offset);
   }-*/;

   public final native void setEndAfter(Node refNode) /*-{
      return this.setEndAfter(refNode);
   }-*/;

   public final native void setEndBefore(Node refNode) /*-{
      return this.setEndBefore(refNode);
   }-*/;

   public final native void setStart(Node refNode, int offset) /*-{
      return this.setStart(refNode, offset);
   }-*/;

   public final native void setStartAfter(Node refNode) /*-{
      return this.setStartAfter(refNode);
   }-*/;

   public final native void setStartBefore(Node refNode) /*-{
      return this.setStartBefore(refNode);
   }-*/;

   public final native void surroundContents(Node newParent) /*-{
      return this.surroundContents(newParent);
   }-*/;

   public final native String toStringJs() /*-{
      return this.toString();
   }-*/;
}
