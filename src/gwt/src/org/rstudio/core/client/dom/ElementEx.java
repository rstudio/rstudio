/*
 * ElementEx.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.user.client.Element;

public class ElementEx extends Element
{
   protected ElementEx()
   {
   }
   
   public final native boolean getContentEditable() /*-{
      return !!this.contentEditable ;
   }-*/;

   public final native void normalize() /*-{
      this.normalize() ;
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

   private final native ElementEx getOwningIFrame() /*-{
      var doc = this.ownerDocument;
      var win = doc.parentWindow || doc.defaultView;
      return win.frameElement;
   }-*/;
}
