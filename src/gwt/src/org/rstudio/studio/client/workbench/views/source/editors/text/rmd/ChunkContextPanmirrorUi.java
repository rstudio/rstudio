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

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode.SyncType;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeEditorSync;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

/**
 * Implementation of a R Markdown chunk context toolbar host for visual mode
 * (inside Panmirror)
 */
public class ChunkContextPanmirrorUi extends ChunkContextUi
{
   public ChunkContextPanmirrorUi(TextEditingTarget outerEditor, Scope outerChunk,
         DocDisplay innerEditor, boolean dark, VisualModeEditorSync sync)
   {
      super(outerEditor, outerChunk, innerEditor, dark);

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
      return outerChunk_.getPreamble().getRow();
   }

   @Override
   protected int getInnerRow()
   {
      // The inner (embedded) editor always has the chunk on the first row
      return 0;
   }

   private final VisualModeEditorSync sync_;
}
