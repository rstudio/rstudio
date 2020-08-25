/*
 * ChunkContextPanmirrorUi.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode.SyncType;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeEditorSync;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

public class ChunkContextPanmirrorUi extends ChunkContextUi
{
   public ChunkContextPanmirrorUi(TextEditingTarget target, 
                                  boolean dark, 
                                  Scope chunk,
                                  VisualModeEditorSync sync)
   {
      super(target, dark, chunk);

      sync_ = sync;

      // Position toolbar at top right of chunk
      Style style = toolbar_.getElement().getStyle();
      style.setTop(20, Unit.PX);
      style.setRight(0, Unit.PX);
      style.setWidth(100, Unit.PX);
   }

   @Override
   public void runChunk()
   {
      sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
      {
         super.runChunk();
      });
   }

   @Override
   public void runPreviousChunks()
   {
      sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
      {
         super.runPreviousChunks();
      });
   }

   @Override
   protected int getRow()
   {
      return chunk_.getPreamble().getRow();
   }
   
   private final VisualModeEditorSync sync_;
}
