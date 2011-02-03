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
}
