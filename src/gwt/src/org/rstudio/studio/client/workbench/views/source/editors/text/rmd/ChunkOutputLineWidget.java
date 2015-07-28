/*
 * ChunkOutputLineWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;

public class ChunkOutputLineWidget extends LineWidget
{   
   protected ChunkOutputLineWidget()
   {
   }
   
   public static final ChunkOutputLineWidget create(int row)
   {
      DivElement div = Document.get().createDivElement();
      div.getStyle().setBackgroundColor("white");
      div.getStyle().setOpacity(1.0);
      div.setInnerText("Here is some output right now");
      
      ChunkOutputLineWidget widget = LineWidget.create(row, div).cast();
      widget.setFixedWidth(true);
      widget.setVisible(true);
      return widget;
   }
   
   public native final boolean getVisible() /*-{
      return this.visible;
   }-*/;

   public native final boolean setVisible(boolean visible) /*-{
      this.visible = visible;
   }-*/;   

   public native final String getHtmlRef() /*-{
      return this.html_ref;
   }-*/;

   public native final void setHtmlRef(String htmlRef) /*-{
      this.html_ref = htmlRef;
   }-*/;
   
 
}
