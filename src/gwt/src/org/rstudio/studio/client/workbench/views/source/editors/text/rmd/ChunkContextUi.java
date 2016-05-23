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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;

import com.google.gwt.regexp.shared.RegExp;

public class ChunkContextUi implements ChunkContextToolbar.Host
{
   public ChunkContextUi(TextEditingTarget target, int renderPass, 
         boolean dark, Scope chunk, PinnedLineWidget.Host lineWidgetHost)
   {
      target_ = target;
      int preambleRow = chunk.getPreamble().getRow();
      preambleRow_ = preambleRow;
      isSetup_ = isSetupChunk(preambleRow);
      isEval_ = isEvalChunk(preambleRow);
      host_ = lineWidgetHost;
      dark_ = dark;
      renderPass_ = renderPass;
      createToolbar(preambleRow);
   }
   
   // Public methods ----------------------------------------------------------

   public int getPreambleRow()
   {
      return lineWidget_.getRow();
   }
   
   public void setState(int state)
   {
      toolbar_.setState(state);
   }
   
   public LineWidget getLineWidget()
   {
      return lineWidget_.getLineWidget();
   }
   
   public void detach()
   {
      lineWidget_.detach();
   }
   
   public void syncToChunk()
   {
      int row = lineWidget_.getRow();
      boolean isSetup = isSetupChunk(row);
      if (isSetup_ != isSetup)
      {
         isSetup_ = isSetup;
         toolbar_.setRunPrevious(!isSetup_);
      }
      boolean isEval = isEvalChunk(row);
      if (isEval != isEval_)
      {
         isEval_ = isEval;
         toolbar_.setRun(isEval);
      }
   }
   
   public void setRenderPass(int pass)
   {
      renderPass_ = pass;
   }
   
   public int getRenderPass()
   {
      return renderPass_;
   }

   // ChunkContextToolbar.Host implementation ---------------------------------
   
   @Override
   public void runPreviousChunks()
   {
      target_.executeChunks(chunkPosition(), 
            TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
      target_.focus();
   }

   @Override
   public void runChunk()
   {
      target_.executeChunk(chunkPosition());
      target_.focus();
   }

   @Override
   public void showOptions(int x, int y)
   {
      ChunkOptionsPopupPanel panel = isSetupChunk(lineWidget_.getRow()) ?
         new SetupChunkOptionsPopupPanel() :
         new DefaultChunkOptionsPopupPanel();
      
      panel.init(target_.getDocDisplay(), chunkPosition());
      panel.show();
      panel.focus();
      PopupPositioner.setPopupPosition(panel, x, y, 10);
   }
   
   @Override
   public void interruptChunk()
   {
      target_.fireEvent(new InterruptChunkEvent(preambleRow_));
   }

   @Override
   public void dequeueChunk()
   {
      RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            "Chunk Pending Execution", 
            "The code in this chunk is scheduled to run later, when other " +
            "chunks have finished executing.", 
            false, // include cancel
            null,  // yes operation,
            new Operation() 
            {
               @Override
               public void execute()
               {
                  target_.dequeueChunk(lineWidget_.getRow());
               }
            }, 
            null,  // cancel operation 
            "OK", 
            "Don't Run", true);
   }

   // Private methods ---------------------------------------------------------
   
   private Position chunkPosition()
   {
      return Position.create(lineWidget_.getRow(), 0);
   }
   
   private boolean isSetupChunk(int row)
   {
      String line = target_.getDocDisplay().getLine(row);
      return line.contains("r setup");
   }
   
   private boolean isEvalChunk(int row) 
   {
      String line = target_.getDocDisplay().getLine(row);
      RegExp evalFalseReg = RegExp.compile("\\beval\\s*=\\s*FALSE\\b");
      return evalFalseReg.exec(line) == null;
   }
   
   private void createToolbar(int row)
   {
      toolbar_ = new ChunkContextToolbar(this, dark_, !isSetup_, isEval_);
      toolbar_.setHeight("0px"); 
      lineWidget_ = new PinnedLineWidget(
            ChunkContextToolbar.LINE_WIDGET_TYPE, target_.getDocDisplay(), 
            toolbar_, row, null, host_);
   }

   private final TextEditingTarget target_;
   private final PinnedLineWidget.Host host_;
   private final int preambleRow_;
   private final boolean dark_;

   private ChunkContextToolbar toolbar_;
   private PinnedLineWidget lineWidget_;
   private int renderPass_;
   
   private boolean isSetup_;
   private boolean isEval_;
}
