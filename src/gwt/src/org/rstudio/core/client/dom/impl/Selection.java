/*
 * Selection.java
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
import com.google.gwt.dom.client.Node;
import org.rstudio.core.client.dom.NativeWindow;

class Selection extends JavaScriptObject
{
   protected Selection()
   {
   }

   public static native Selection get() /*-{
      return $wnd.getSelection();
   }-*/;

   public static native Selection get(NativeWindow window) /*-{
      return window.getSelection();
   }-*/;

   public final native int getRangeCount() /*-{
      return this.rangeCount;
   }-*/;

   public final native Range getRangeAt(int index) /*-{
      return this.getRangeAt(index);
   }-*/;

   public final native void removeAllRanges() /*-{
      return this.removeAllRanges();
   }-*/;

   public final native void addRange(Range range) /*-{
      return this.addRange(range);
   }-*/;

   public final native Node getAnchorNode() /*-{
      return this.anchorNode;
   }-*/;

   public final native int getAnchorOffset() /*-{
      return this.anchorOffset;
   }-*/;

   public final native Node getFocusNode() /*-{
      return this.focusNode;
   }-*/;

   public final native int getFocusOffset() /*-{
      return this.focusOffset;
   }-*/;

   public final void setRange(Range selection)
   {
      removeAllRanges();
      if (selection != null)
         addRange(selection);
   }
}
