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
}
