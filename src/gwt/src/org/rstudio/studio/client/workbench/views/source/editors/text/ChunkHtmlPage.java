/*
 * ChunkHtmlPage.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class ChunkHtmlPage extends ChunkOutputPage
                           implements EditorThemeListener
{
   public ChunkHtmlPage(String url, NotebookHtmlMetadata metadata,
         int ordinal, final Command onRenderComplete, ChunkOutputSize chunkOutputSize)
   {
      super(ordinal);

      // extract classes from metadata if present
      JsArrayString classes = JsArrayString.createArray().cast();
      if (metadata != null) 
         classes = metadata.getClasses();

      String clazz = classes.length() > 0 ? classes.get(0) : "html";
      thumbnail_ = new ChunkOutputThumbnail(clazz, 
            classes.length() > 1 ? classes.get(1) : "",
            new ChunkHtmlPreview(), ChunkOutputWidget.getEditorColors());

      // amend the URL to cause any contained widget to use the RStudio viewer
      // sizing policy
      if (url.indexOf('?') > 0)
         url += "&";
      else
         url += "?";
      url += "viewer_pane=1&capabilities=1";

      frame_ = new ChunkOutputFrame("Chunk HTML Page Output Frame");
      
      if (chunkOutputSize != ChunkOutputSize.Full) {
         content_ = new FixedRatioWidget(frame_, 
               ChunkOutputUi.OUTPUT_ASPECT, 
               ChunkOutputUi.MAX_HTMLWIDGET_WIDTH);
      }
      else {
         frame_.getElement().getStyle().setWidth(100, Unit.PCT);
         content_ = frame_;
      }

      frame_.loadUrlDelayed(url, 400, new Command() 
      {
         @Override
         public void execute()
         {
            Element body = frame_.getDocument().getBody();
            Style bodyStyle = body.getStyle();
      
            bodyStyle.setPadding(0, Unit.PX);
            bodyStyle.setMargin(0, Unit.PX);

            onEditorThemeChanged(ChunkOutputWidget.getEditorColors());

            Timer frameFinishLoadTimer = new Timer()
            {
               @Override
               public void run()
               {
                  onRenderComplete.execute();
               }
            };

            frameFinishLoadTimer.schedule(100);
         }
      });

      afterRender_ = new Command() {
         @Override
         public void execute()
         {
            Element body = frame_.getDocument().getBody();

            Style bodyStyle = body.getStyle();
            bodyStyle.setPadding(0, Unit.PX);
            bodyStyle.setMargin(0, Unit.PX);

            syncThemeTextColor(themeColors_, body);
         }
      };

      Event.sinkEvents(frame_.getElement(), Event.ONLOAD);
      Event.setEventListener(frame_.getElement(), e ->
      {
         if (Event.ONLOAD == e.getTypeInt() && themeColors_ != null)
         {
            afterRender_.execute();
         }
      });
   }
   
   public static void syncThemeTextColor(Colors themeColors, Element body)
   {
      Style bodyStyle = body.getStyle();
      if (StringUtil.isNullOrEmpty(bodyStyle.getBackgroundColor()) &&
          StringUtil.isNullOrEmpty(body.getClassName()))
      {
         bodyStyle.setColor(themeColors.foreground);
      }
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
      frame_.runAfterRender(afterRender_);
   }

   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      themeColors_ = colors;
      afterRender_.execute();
   }

   private ChunkOutputFrame frame_;
   final private Widget thumbnail_;
   final private Widget content_;
   private Colors themeColors_ = null;
   private Command afterRender_;
}
