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

public class ChunkOutputPanmirrorUi extends ChunkOutputUi
{
   public ChunkOutputPanmirrorUi(String docId, VisualMode visualMode, ChunkDefinition def)
   {
      super(docId, def);
      
      // Find the visual mode chunk editor associated with this row of the document
      chunk_ = visualMode.getChunkAtRow(def.getRow());
      
      ChunkOutputWidget widget = getOutputWidget();
      widget.setEmbeddedStyle(true);

      // If we found one, hook it up to the output widget
      if (chunk_ != null)
      {
         chunk_.setOutputWidget(widget);
      }
   }

   @Override
   public void onOutputHeightChanged(ChunkOutputWidget widget, int height, boolean ensureVisible)
   {
      
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
