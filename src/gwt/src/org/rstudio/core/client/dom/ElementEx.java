/*
 * ElementEx.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.Element;

public class ElementEx extends Element
{
   protected ElementEx()
   {
   }

   public final native boolean getContentEditable() /*-{
      return !!this.contentEditable;
   }-*/;

   public final native void normalize() /*-{
      this.normalize();
   }-*/;

   public final native String getOuterHtml() /*-{
      if (typeof(this.outerHTML) != 'undefined')
         return this.outerHTML;

      // Firefox does not support the outerHTML property
      var copy = this.cloneNode(true);
      var tmpContainer = document.createElement(this.parentNode.tagName);
      tmpContainer.appendChild(copy);
      return tmpContainer.innerHTML;
   }-*/;

   public final native String getAttribute(String attribName, int mode) /*-{
      var result = this.getAttribute(attribName, mode);
      return (result == null) ? '' : result + '';
   }-*/;

   public final int getClientLeft()
   {
      int left = getAbsoluteLeft();
      ElementEx iFrame = getOwningIFrame();
      if (iFrame != null)
         left += iFrame.getClientLeft();
      return left;
   }

   public final int getClientTop()
   {
      int top = getAbsoluteTop();
      ElementEx iFrame = getOwningIFrame();
      if (iFrame != null)
         top += iFrame.getClientTop();
      return top;
   }

   // NOTE: these static methods are provided only because
   // GWT seems unable to find the instance methods of the
   // same name in some contexts in devmode
   public static final int getClientLeft(Element el)
   {
      return ((ElementEx) el).getClientLeft();
   }

   public static final int getClientTop(Element el)
   {
      return ((ElementEx) el).getClientTop();
   }

   public static final DOMRect getBoundingClientRect(Element el)
   {
      return ((ElementEx) el).getBoundingClientRect();
   }

   public final native DOMRect getBoundingClientRect() /*-{
      return this.getBoundingClientRect();
   }-*/;

   private final native ElementEx getOwningIFrame() /*-{
      var doc = this.ownerDocument;
      var win = doc.parentWindow || doc.defaultView;
      return win.frameElement;
   }-*/;
}
