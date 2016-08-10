/*
 * ChunkHtmlPage.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class ChunkHtmlPage implements ChunkOutputPage
{
   public ChunkHtmlPage(String url, NotebookHtmlMetadata metadata,
         final Command onRenderComplete)
   {
      String clazz = metadata.getClasses().length() > 0 ? 
            metadata.getClasses().get(0) : "html";
      thumbnail_ = new ChunkOutputThumbnail(clazz, "htmlwidget", null,
            ChunkOutputWidget.getEditorColors());

      // amend the URL to cause any contained widget to use the RStudio viewer
      // sizing policy
      if (url.indexOf('?') > 0)
         url += "&";
      else
         url += "?";
      url += "viewer_pane=1";

      final ChunkOutputFrame frame = new ChunkOutputFrame();
      content_= new FixedRatioWidget(frame, 
                  ChunkOutputUi.OUTPUT_ASPECT, 
                  ChunkOutputUi.MAX_HTMLWIDGET_WIDTH);

      frame.loadUrl(url, new Command() 
      {
         @Override
         public void execute()
         {
            Element body = frame.getDocument().getBody();
            Style bodyStyle = body.getStyle();
            
            bodyStyle.setPadding(0, Unit.PX);
            bodyStyle.setMargin(0, Unit.PX);
            bodyStyle.setColor(ChunkOutputWidget.getEditorColors().foreground);
            
            onRenderComplete.execute();
         };
      });
   }
      
   @Override
   public Widget thumbnailWidget()
   {
      return thumbnail_;
   }

   @Override
   public Widget contentWidget()
   {
      return content_;
   }

   @Override
   public void onSelected()
   {
      // no action necessary for HTML widgets
   }

   final private Widget thumbnail_;
   final private Widget content_;
}
