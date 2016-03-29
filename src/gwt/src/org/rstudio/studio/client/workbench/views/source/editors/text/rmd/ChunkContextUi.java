/*
 * ChunkContextUi.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class ChunkContextUi implements ChunkContextToolbar.Host
{
   public ChunkContextUi(TextEditingTarget target, Scope chunk)
   {
      target_ = target;
      isSetup_ = false;
      toolbar_ = new ChunkContextToolbar(this, false, 
            true, true);
      toolbar_.setHeight("0px"); 
      widget_ = LineWidget.create(
            ChunkContextToolbar.LINE_WIDGET_TYPE, 
            chunk.getPreamble().getRow(), toolbar_.getElement());
      widget_.setFixedWidth(true); 
      target.getDocDisplay().addLineWidget(widget_);
   }

   @Override
   public void runPreviousChunks()
   {
      target_.executePreviousChunks(chunkPosition());
   }

   @Override
   public void runChunk()
   {
      target_.executeChunk(chunkPosition());
   }

   @Override
   public void showOptions(int x, int y)
   {
      ChunkOptionsPopupPanel panel = isSetup_ ?
         new SetupChunkOptionsPopupPanel() :
         new DefaultChunkOptionsPopupPanel();
      
      panel.init(target_.getDocDisplay(), chunkPosition());
      panel.show();
      panel.focus();
      PopupPositioner.setPopupPosition(panel, x, y, 10);
   }
   
   // Private methods ---------------------------------------------------------
   
   private Position chunkPosition()
   {
      return Position.create(widget_.getRow(), 0);
   }
   
   private final TextEditingTarget target_;
   private final ChunkContextToolbar toolbar_;
   private final LineWidget widget_;
   private final boolean isSetup_;
}
