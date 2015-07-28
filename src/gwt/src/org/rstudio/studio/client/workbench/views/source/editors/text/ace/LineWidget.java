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
   protected LineWidget() {}
 
   public static final LineWidget create(int row, String html)
   {
      return create(row, html, null);
   }
   
   public static final LineWidget create(int row, 
                                         String html,
                                         JavaScriptObject data)
   {
      return create(row, html, null, data);
   }
   
   public static final LineWidget create(int row, Element el)
   {
      return create(row, el, null);
   }
   
   public static final LineWidget create(int row, 
                                         Element el, 
                                         JavaScriptObject data)
   {
      return create(row, null, el, data);
   }
   
   private static native final LineWidget create(int row, 
                                                 String html,
                                                 Element element,
                                                 JavaScriptObject data) /*-{
      return {
         row: row,
         html: html,
         el: element,
         data: data
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
   
   public native final void setPixelHeight(int pixelHeight) /*-{
      this.pixelHeight = pixelHeight;
   }-*/;

   public native final int getRowCount()  /*-{
      return this.rowCount;
   }-*/;
   
   public native final void setRowCount(int rowCount) /*-{
      this.rowCount = rowCount;
   }-*/;

   public native final boolean getFixedWidth()  /*-{
      return this.fixedWidth;
   }-*/;
   
   public native final void setFixedWidth(boolean fixedWidth) /*-{
      this.fixedWidth = fixedWidth;
   }-*/;

   public native final boolean getCoverGutter()  /*-{
      return this.coverGutter;
   }-*/;
   
   public native final void setCoverGutter(boolean coverGutter) /*-{
      this.coverGutter = coverGutter;
   }-*/;
   
   public final native <T> T getData() /*-{
      return this.data;
   }-*/;
   
   public final native void setData(JavaScriptObject data) /*-{
      this.data = data;
   }-*/;
}
