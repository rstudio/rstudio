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

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

public class TextEditingTargetNotebook
{
   public TextEditingTargetNotebook(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;      
   }
   
   public void initialize(SourceDocument document)
   {
      // if there is chunk output then use it to reconstruct
      // the chunk output line widgets for the document
      JsArray<ChunkOutput> chunkOutputs = document.getChunkOutput();
      for (int i = 0; i<chunkOutputs.length(); i++)
      {
         ChunkOutput chunkOutput = chunkOutputs.get(i);
         LineWidget widget = LineWidget.create(
               ChunkOutput.LINE_WIDGET_TYPE,
               chunkOutput.getRow(), 
               elementForChunkOutput(chunkOutput), 
               chunkOutput);
         widget.setFixedWidth(true);
         docDisplay_.addLineWidget(widget);
      }
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
   }
   
   private DivElement elementForChunkOutput(ChunkOutput chunkOutput)
   {
      DivElement div = Document.get().createDivElement();
      div.getStyle().setBackgroundColor("white");
      div.getStyle().setOpacity(1.0);
      setChunkOutput(div);
      return div;
   }
   
   private void setChunkOutput(Element div)
   {
      div.setInnerText(Document.get().createUniqueId());
   }
   
   private final DocDisplay docDisplay_;
   @SuppressWarnings("unused")
   private final DocUpdateSentinel docUpdateSentinel_;
}
