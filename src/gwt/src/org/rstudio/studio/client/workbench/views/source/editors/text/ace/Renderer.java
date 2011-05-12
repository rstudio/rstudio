/*
 * Renderer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Renderer extends JavaScriptObject
{
   public static class ScreenCoordinates extends JavaScriptObject
   {
      protected ScreenCoordinates() {}

      public native final int getPageX() /*-{
         return this.pageX;
      }-*/;

      public native final int getPageY() /*-{
         return this.pageY;
      }-*/;
   }

   protected Renderer() {}

   public native final ScreenCoordinates textToScreenCoordinates(int row,
                                                                 int col) /*-{
      return this.textToScreenCoordinates(row, col);
   }-*/;

   public native final void forceScrollbarUpdate() /*-{
      // WebKit-based browsers have problems repainting the scrollbar after
      // the editor is hidden and then made visible again. Poking the style
      // a little bit seems to force a redraw.
      var style = this.scrollBar.element.style;
      style.marginBottom = (style.marginBottom == "-1px") ? "0" : "-1px";     
   }-*/;

   public native final void updateFontSize() /*-{
      this.updateFontSize();
   }-*/;

   public native final void onResize(boolean force) /*-{
      this.onResize(force);
   }-*/;

   public native final void setHScrollBarAlwaysVisible(boolean on) /*-{
      this.setHScrollBarAlwaysVisible(on);
   }-*/;

   public native final void setShowGutter(boolean on) /*-{
      this.setShowGutter(on);
   }-*/;

   public native final void setShowPrintMargin(boolean on) /*-{
      this.setShowPrintMargin(on);
   }-*/;

   public native final void setPrintMarginColumn(int column) /*-{
      this.setPrintMarginColumn(column);
   }-*/;

   public native final void setPadding(int padding) /*-{
      this.setPadding(padding);
   }-*/;

   public native final int getLineHeight() /*-{
      return this.lineHeight;
   }-*/;
}
