/*
 * ElementEx.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

   // NOTE: We intentionally avoid calling this method 'getClientLeft()',
   // as it seems to collide with other GWT definitions and causes GWT to
   // fail to see the function definition.
   public final int clientLeft()
   {
      int left = getAbsoluteLeft();
      ElementEx iFrame = getOwningIFrame();
      if (iFrame != null)
         left += iFrame.clientLeft();
      return left;
   }

   // NOTE: We intentionally avoid calling this method 'getClientTop()',
   // as it seems to collide with other GWT definitions and causes GWT to
   // fail to see the function definition.
   public final int clientTop()
   {
      int top = getAbsoluteTop();
      ElementEx iFrame = getOwningIFrame();
      if (iFrame != null)
         top += iFrame.clientTop();
      return top;
   }

   public static final int clientLeft(Element el)
   {
      return ((ElementEx) el).clientLeft();
   }

   public static final int clientTop(Element el)
   {
      return ((ElementEx) el).clientTop();
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
