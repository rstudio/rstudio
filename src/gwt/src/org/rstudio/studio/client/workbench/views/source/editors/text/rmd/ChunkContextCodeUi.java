/*
 * ChunkContextCodeUi.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;

/**
 * Implementation of a R Markdown chunk context toolbar host for code view
 * (inside the text editor)
 */
public class ChunkContextCodeUi extends ChunkContextUi
{
   public ChunkContextCodeUi(TextEditingTarget target, boolean dark, Scope chunk,
                             PinnedLineWidget.Host host, int renderPass)
   {
      super(target, chunk, target.getDocDisplay(), dark);
      
      host_ = host;
      renderPass_ = renderPass;
   }

   @Override
   protected int getRow()
   {
      return lineWidget_.getRow();
   }

   @Override
   protected int getInnerRow()
   {
      return getRow();
   }

   @Override
   protected void createToolbar(int row)
   {
      super.createToolbar(row);
      lineWidget_ = new PinnedLineWidget(
            ChunkContextToolbar.LINE_WIDGET_TYPE, outerEditor_.getDocDisplay(), 
            toolbar_, row, null, host_);
   }
   
   @Override
   public void runChunk()
   {
      super.runChunk();
      outerEditor_.focus();
   }

   @Override
   public void runPreviousChunks()
   {
      super.runPreviousChunks();
      outerEditor_.focus();
   }

   public LineWidget getLineWidget()
   {
      return lineWidget_.getLineWidget();
   }
   
   public void detach()
   {
      lineWidget_.detach();
   }

   public void setRenderPass(int pass)
   {
      renderPass_ = pass;
   }
   
   public int getRenderPass()
   {
      return renderPass_;
   }

   private PinnedLineWidget lineWidget_;

   private int renderPass_;
   
   private final PinnedLineWidget.Host host_;
}
