/*
 * LineWidget.java
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

public class LineWidget extends JavaScriptObject
{
   public static class Options extends JavaScriptObject
   {
      protected Options()
      {
      }
      
      public static native Options create() /*-{
         return {
            pixelHeight: null,
            rowCount: null,
            fixedWidth: null,
            coverGutter: null
         };
      }-*/;
      
      public native final void setPixelHeight(int pixelHeight) /*-{
         this.pixelHeight = pixelHeight;
      }-*/;
      
      public native final void setRowCount(int rowCount) /*-{
         this.rowCount = rowCount;
      }-*/;
      
      public native final void setFixedWidth(boolean fixedWidth) /*-{
         this.fixedWidth = fixedWidth;
      }-*/;
      
      public native final void setCoverGutter(boolean coverGutter) /*-{
         this.coverGutter = coverGutter;
      }-*/;
   }
   
   protected LineWidget() {}
 
   public static final LineWidget create(int row, String html, Options options)
   {
      return create(row, html, null, options);
   }
   
   public static final LineWidget create(int row, Element el, Options options)
   {
      return create(row, null, el, options);
   }
   
   private static native final LineWidget create(int row, 
                                                 String html,
                                                 Element element,
                                                 Options options) /*-{
      return {
         row: row,
         html: html,
         el: element,   
         pixelHeight: options.pixelHeight,
         rowCount: options.rowCount,
         fixedWidth: options.fixedWidth,
         coverGutter: options.coverGutter
      };
   }-*/;
   
   public native final int getRow()  /*-{
      return this.row;
   }-*/;
   
   public native final Element getElement() /*-{
      return this.el;
   }-*/;
   
   public native final int getPixelHeight()  /*-{
      return this.pixelHeight;
   }-*/;
   
   public native final int getRowCount()  /*-{
      return this.rowCount;
   }-*/;
   
   public native final boolean getFixedWidth()  /*-{
      return this.fixedWidth;
   }-*/;
   
   public native final boolean getCoverGutter()  /*-{
      return this.coverGutter;
   }-*/;
}
