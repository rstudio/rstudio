/*
 * Renderer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public class Renderer extends JavaScriptObject
{
   public static class ScreenCoordinates extends JavaScriptObject
   {
      protected ScreenCoordinates() {}
      
      public native final static ScreenCoordinates create(int pageX, int pageY) /*-{
         return {
            pageX: pageX,
            pageY: pageY
         };
      }-*/;

      public native final int getPageX() /*-{
         return Math.round(this.pageX);
      }-*/;

      public native final int getPageY() /*-{
         return Math.round(this.pageY);
      }-*/;
   }

   protected Renderer() {}

   public native final ScreenCoordinates textToScreenCoordinates(int row,
                                                                 int col) /*-{
      return this.textToScreenCoordinates(row, col);
   }-*/;
   
   public final ScreenCoordinates textToScreenCoordinates(Position pos)
   {
      return textToScreenCoordinates(pos.getRow(), pos.getColumn());
   }
   
   public native final Position screenToTextCoordinates(int pageX, int pageY) /*-{
      return this.screenToTextCoordinates(pageX, pageY);
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
   
   public native final void updateFull(boolean force) /*-{
      this.updateFull(force);
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
   
   public native final void setShowInvisibles(boolean show) /*-{
      this.setShowInvisibles(show); 
   }-*/;
   
   public native final void setShowIndentGuides(boolean show) /*-{
      this.setDisplayIndentGuides(show); 
   }-*/;
   
   public native final void setBlinkingCursor(boolean blinking) /*-{
      this.$cursorLayer.setBlinking(blinking);
   }-*/;

   public native final void setPadding(int padding) /*-{
      this.setPadding(padding);
   }-*/;

   public native final int getLineHeight() /*-{
      return this.lineHeight;
   }-*/;

   public native final double getCharacterWidth() /*-{
      return this.characterWidth;
   }-*/;

   public native final Element getCursorElement() /*-{
      return this.$cursorLayer.cursor;
   }-*/;

   public native final int getScrollTop() /*-{
      return this.getScrollTop() || 0;
   }-*/;

   public native final int getScrollLeft() /*-{
      return this.getScrollLeft() || 0;
   }-*/;

   public native final void scrollToY(int scrollTop) /*-{
      this.scrollToY(scrollTop);
   }-*/;

   public native final void scrollToX(int scrollLeft) /*-{
      this.scrollToX(scrollLeft);
   }-*/;
   
   public native final void setAnimatedScroll(boolean animate) /*-{
      this.setAnimatedScroll(animate);
   }-*/;
   
   public native final boolean getAnimatedScroll() /*-{
      return this.getAnimatedScroll();
   }-*/;

   public native final void forceImmediateRender() /*-{
      this.$renderChanges(this.CHANGE_FULL);
   }-*/;
   
   public native final void renderMarkers() /*-{
      this.$renderChanges(this.CHANGE_MARKER);
   }-*/;

   public native final void fixVerticalOffsetBug() /*-{
      this.scroller.scrollTop = 0;
   }-*/;

   public native final void setPasswordMode(boolean passwordMode) /*-{

      if (passwordMode)
      {
         this.characterWidth = 0;
         this.$textLayer.element.style.visibility = 'hidden';
         this.$renderChanges(this.CHANGE_FULL);
      }
      else
      {
         this.characterWidth = this.$textLayer.getCharacterWidth();
         this.$textLayer.element.style.visibility = 'visible';
         this.$renderChanges(this.CHANGE_FULL);
      }
   }-*/;
   
   public native final void addGutterDecoration(int line, String clazz) /*-{
      this.session.addGutterDecoration(line, clazz);
   }-*/;
   
   public native final void removeGutterDecoration(int line, String clazz) /*-{
      this.session.removeGutterDecoration(line, clazz);
   }-*/;
   
   public final native void alignCursor(Position position, double ratio) /*-{
      this.alignCursor(position, ratio);
   }-*/;
   
   public final native void setScrollPastEnd(boolean value) /*-{
      this.$scrollPastEnd = value;
   }-*/;
   
   public final native boolean getScrollPastEnd() /*-{
      return !!this.$scrollPastEnd;
   }-*/;
   
   public final native int getFirstFullyVisibleRow() /*-{
      return this.getFirstFullyVisibleRow();
   }-*/;
   
}
