/*
 * TextEditingTargetNotebook.java
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

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.inject.Inject;

public class TextEditingTargetNotebook
{
   public TextEditingTargetNotebook(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
      initialChunkOutputs_ = document.getChunkOutput();
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // single shot rendering of chunk output line widgets
      // (we wait until after the first render to ensure that
      // ace places the line widgets correctly)
      docDisplay_.addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      { 
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            if (initialChunkOutputs_ != null)
            {
               for (int i = 0; i<initialChunkOutputs_.length(); i++)
               {
                  ChunkOutput chunkOutput = initialChunkOutputs_.get(i);
                  LineWidget widget = LineWidget.create(
                        ChunkOutput.LINE_WIDGET_TYPE,
                        chunkOutput.getRow(), 
                        elementForChunkOutput(chunkOutput), 
                        chunkOutput);
                  widget.setFixedWidth(true);
                  docDisplay_.addLineWidget(widget);
               }
               initialChunkOutputs_ = null;
            }
         }
      });
   }
   
   @Inject
   public void initialize(EventBus events)
   {
      events_ = events;
   }
     
   public void executeChunk(Scope chunk, String code)
   {
      int row = chunk.getEnd().getRow();
      
      // if there is an existing widget just modify it in place
      LineWidget existingWidget = docDisplay_.getLineWidgetForRow(row);
      if (existingWidget != null && 
          existingWidget.getType().equals(ChunkOutput.LINE_WIDGET_TYPE))
      {
         setChunkOutput(existingWidget.getElement());
         docDisplay_.onLineWidgetChanged(existingWidget);
      }
      // otherwise create a new one
      else
      {
         ChunkOutput chunkOutput = ChunkOutput.create(row, 1, true, "ref");
        
         LineWidget widget = LineWidget.create(
                               ChunkOutput.LINE_WIDGET_TYPE,
                               row, 
                               elementForChunkOutput(chunkOutput), 
                               chunkOutput);
         widget.setFixedWidth(true);
         docDisplay_.addLineWidget(widget);
      }
      
      // still execute in console
      events_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   private DivElement elementForChunkOutput(ChunkOutput chunkOutput)
   {
      DivElement div = Document.get().createDivElement();
      div.addClassName(ThemeStyles.INSTANCE.selectableText());
      div.getStyle().setBackgroundColor("white");
      div.getStyle().setOpacity(1.0);
      setChunkOutput(div);
      return div;
   }
   
   private void setChunkOutput(Element div)
   {
      div.setInnerText(Document.get().createUniqueId());
   }
  
   
   private JsArray<ChunkOutput> initialChunkOutputs_;
   
   private final DocDisplay docDisplay_;
   @SuppressWarnings("unused")
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private EventBus events_;
}
