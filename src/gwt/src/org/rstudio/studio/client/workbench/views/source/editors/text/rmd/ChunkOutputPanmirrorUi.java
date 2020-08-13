/*
 * ChunkOutputPanmirrorUi.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeChunk;

import com.google.gwt.dom.client.Style.Unit;

/**
 * A host for notebook chunk output for visual editing mode; it wraps a
 * ChunkOutputWidget. It is complemented by ChunkOutputCodeUi, which wraps chunk
 * output in code (raw text) mode; these classes can both wrap the same output
 * widget, and trade ownership of it via detach/reattach.
 */
public class ChunkOutputPanmirrorUi extends ChunkOutputUi
{
   public ChunkOutputPanmirrorUi(String docId, VisualMode visualMode, ChunkDefinition def, 
                                 ChunkOutputWidget widget, VisualModeChunk chunk)
   {
      super(docId, def, widget);
      
      if (chunk == null)
      {
         // Find the visual mode chunk editor associated with this row of the
         // document, if unknown
         chunk_ = visualMode.getChunkAtRow(def.getRow());
      }
      else
      {
         chunk_ = chunk;
      }
      
      ChunkOutputWidget outputWidget = getOutputWidget();
      outputWidget.setEmbeddedStyle(true);

      // If we found one, hook it up to the output widget
      if (chunk_ != null)
      {
         chunk_.setOutputWidget(outputWidget);
         chunk_.setDefinition(def);
      }
   }
   
   public ChunkOutputPanmirrorUi(ChunkOutputCodeUi codeOutput, VisualMode visualMode, 
                                 VisualModeChunk chunk)
   {
      this(codeOutput.getDocId(), visualMode, codeOutput.getDefinition(), 
            codeOutput.getOutputWidget(), chunk);
   }

   @Override
   public void onOutputHeightChanged(ChunkOutputWidget widget, int outputHeight, boolean ensureVisible)
   {
      // don't process if we aren't attached 
      if (!attached_)
         return;

      int height = 
            widget.getExpansionState() == ChunkOutputWidget.COLLAPSED ?
               CHUNK_COLLAPSED_HEIGHT :
               Math.max(MIN_CHUNK_HEIGHT, outputHeight);

      widget.getElement().getStyle().setHeight(height, Unit.PX);
   }

   @Override
   public void onRenderFinished(RenderFinishedEvent event)
   {

   }

   @Override
   public int getCurrentRow()
   {
      if (chunk_ != null)
      {
         return chunk_.getScope().getEnd().getRow();
      }
      return 0;
   }

   @Override
   public void ensureVisible()
   {
      // This is used in code view to scroll the widget into view; currently we
      // don't replicate that behavior in visual mode.
   }

   @Override
   public void detach()
   {
      if (!attached_)
         return;

      chunk_.removeWidget();

      attached_ = false;
   }

   @Override
   public void reattach()
   {
      if (attached_)
         return;
      
      outputWidget_.setEmbeddedStyle(true);
      chunk_.reloadWidget();

      attached_ = true;
   }

   @Override
   public Scope getScope()
   {
      if (chunk_ == null)
         return null;
      
      return chunk_.getScope();
   }
   
   private final VisualModeChunk chunk_;
}
