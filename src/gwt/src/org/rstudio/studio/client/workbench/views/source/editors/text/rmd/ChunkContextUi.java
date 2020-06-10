/*
 * ChunkContextUi.java
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

import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.CustomEngineChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.SetupChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;

import com.google.gwt.core.client.JsArrayString;

public class ChunkContextUi implements ChunkContextToolbar.Host
{
   public ChunkContextUi(TextEditingTarget target, int renderPass, 
         boolean dark, Scope chunk, PinnedLineWidget.Host lineWidgetHost)
   {
      target_ = target;
      chunk_ = chunk;
      int preambleRow = chunk.getPreamble().getRow();
      preambleRow_ = preambleRow;
      isSetup_ = isSetupChunk(preambleRow);
      host_ = lineWidgetHost;
      dark_ = dark;
      renderPass_ = renderPass;
      engine_ = getEngine(preambleRow);
      createToolbar(preambleRow);
   }
   
   // Public static methods ---------------------------------------------------

   public static String extractChunkLabel(String extractedChunkHeader)
   {
      // if there are no spaces within the chunk header,
      // there cannot be a label
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return "";

      // find the indices of the first '=' and ',' characters
      int firstEqualsIdx = extractedChunkHeader.indexOf('=');
      int firstCommaIdx  = extractedChunkHeader.indexOf(',');

      // if we found neither an '=' nor a ',', then the label
      // must be all the text following the first space
      if (firstEqualsIdx == -1 && firstCommaIdx == -1)
      {
         extractedChunkHeader = extractedChunkHeader.substring(firstSpaceIdx + 1).trim();
         if (extractedChunkHeader.endsWith("}"))
            extractedChunkHeader = extractedChunkHeader.substring(0, extractedChunkHeader.length() -1);
         return extractedChunkHeader;
      }

      // if we found an '=' before we found a ',' (or we didn't find
      // a ',' at all), that implies a chunk header like:
      //
      //    ```{r message=TRUE, echo=FALSE}
      //
      // and so there is no label.
      if (firstCommaIdx == -1)
         return "";

      if (firstEqualsIdx != -1 && firstEqualsIdx < firstCommaIdx)
         return "";

      // otherwise, the text from the first space to that comma gives the label
      return extractedChunkHeader.substring(firstSpaceIdx + 1, firstCommaIdx).trim();
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
      String engine = getEngine(row);
      if (engine != engine_)
      {
         engine_ = engine;
         toolbar_.setEngine(engine);
      }
      toolbar_.setClassId(getLabel(row));
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
      ChunkOptionsPopupPanel panel = createPopupPanel();
      
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

   @Override
   public void switchChunk(String chunkType)
   {
      if (chunk_ != null)
      {
         DocDisplay docDisplay = target_.getDocDisplay();
         
         Position start = chunk_.getPreamble();
         Position end = chunk_.getEnd();
         
         String chunkText = docDisplay.getTextForRange(Range.fromPoints(start, end));
         JsArrayString chunkLines = StringUtil.split(chunkText, "\n");
         if (chunkLines.length() > 0)
         {
            String firstLine = chunkLines.get(0);
            Position linedEnd = Position.create(start.getRow(),firstLine.length());
            
            String newFirstLine = firstLine.replaceFirst("[, ]*engine='[a-zA-Z]+'", "");
            newFirstLine = newFirstLine.replaceFirst("{[a-zA-Z]+", "{" + chunkType);

            docDisplay.replaceRange(Range.fromPoints(start, linedEnd), newFirstLine);
            
            target_.getNotebook().clearChunkOutput(chunk_);
         }
      }
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
   
   private void createToolbar(int row)
   {
      toolbar_ = new ChunkContextToolbar(this, dark_, !isSetup_, engine_);
      toolbar_.setHeight("0px"); 
      toolbar_.setClassId(getLabel(row));
      lineWidget_ = new PinnedLineWidget(
            ChunkContextToolbar.LINE_WIDGET_TYPE, target_.getDocDisplay(), 
            toolbar_, row, null, host_);
   }

   private String getEngine(int row)
   {
      String line = target_.getDocDisplay().getLine(row);
      Map<String, String> options = RChunkHeaderParser.parse(line);
      String engine = StringUtil.stringValue(options.get("engine"));
      return engine;
   }
   
   private String getLabel(int row)
   {
      String line = target_.getDocDisplay().getLine(row);
      return extractChunkLabel(line);
   }

   private ChunkOptionsPopupPanel createPopupPanel()
   {
      int row = lineWidget_.getRow();
      if (isSetupChunk(row))
         return new SetupChunkOptionsPopupPanel();
      
      String engine = getEngine(row);
      if (!engine.toLowerCase().equals("r") &&
          !engine.toLowerCase().equals("d3"))
         return new CustomEngineChunkOptionsPopupPanel(engine_);
      
      return new DefaultChunkOptionsPopupPanel(engine_);
   }

   private final TextEditingTarget target_;
   private final PinnedLineWidget.Host host_;
   private final int preambleRow_;
   private final boolean dark_;
   private final Scope chunk_;

   private ChunkContextToolbar toolbar_;
   private PinnedLineWidget lineWidget_;
   private int renderPass_;
   
   private boolean isSetup_;
   private String engine_;
}
