/*
 * ChunkContextUi.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;


import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue.OptionLocation;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.CustomEngineChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.SetupChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArrayString;

public abstract class ChunkContextUi implements ChunkContextToolbar.Host
{
   public ChunkContextUi(TextEditingTarget outerEditor, 
                         Scope outerChunk,
                         DocDisplay innerEditor,
                         boolean dark)
   {
      outerEditor_ = outerEditor;
      outerChunk_ = outerChunk;
      innerEditor_ = innerEditor;
      int preambleRow = outerChunk.getPreamble().getRow();
      isSetup_ = isSetupChunk(preambleRow);
      dark_ = dark;
      engine_ = getEngine(preambleRow);
      createToolbar(preambleRow);
   }
   
   // Public static methods ---------------------------------------------------

   /**
    * Helper for returning results from extractChunkLabel
    */
   public static class ChunkLabelInfo
   {
      public ChunkLabelInfo(String label, int nextSepIndex)
      {
         this.label = label;
         this.nextSepIndex = nextSepIndex;
      }

      public String label;
      public int nextSepIndex;
   }

   /**
    * Helper for extractChunkLabel to find the first separator, which can be
    * a space, a comma, or a comma followed by a space

    * @param header
    * @return index of last character of first separator or -1 if no separator
    */
   private static int findFirstSeparator(String header)
   {
      // find first space or comma
      int firstSpaceIdx = header.indexOf(',');
      int firstCommaIdx = header.indexOf(' ');

      // no separators?
      if (firstSpaceIdx == -1 && firstCommaIdx == -1)
      {
         return -1;
      }

      // start at the first one encountered
      int result;
      if (firstSpaceIdx == -1)
      {
         result = firstCommaIdx;
      }
      else if (firstCommaIdx == -1)
      {
         result = firstSpaceIdx;
      }
      else
      {
         result = Math.min(firstSpaceIdx, firstCommaIdx);
      }
      for (int i = result + 1; i < header.length(); i++)
      {
         char ch = header.charAt(i);
         if (ch == ' ' || ch == ',')
         {
            result = i;
         }
         else
         {
            break;
         }
      }
      return result;
   }

   /**
    * Extract a label from a chunk header and return the position of the separator
    * (space or comma) after the label (or the first separator if no label).
    * 
    * Note: Ignores "label=foo" form (i.e. treats it like any other property).
    *
    * @param extractedChunkHeader
    * @return Label string and the index where to continue parsing
    */
   public static ChunkLabelInfo extractChunkLabel(String extractedChunkHeader)
   {
      int firstSeparatorIdx = findFirstSeparator(extractedChunkHeader);

      // no separators implies no label
      if (firstSeparatorIdx == -1)
      {
         // ```{r}
         return new ChunkLabelInfo("", extractedChunkHeader.length());
      }

      // find index of first equals character
      int firstEqualsIdx = extractedChunkHeader.indexOf('=');

      // if no '=' then the label must be the text following the separator
      if (firstEqualsIdx == -1)
      {
         // ```{r label}
         // ```{r,label}
         // ```{r, label}
         int originalLength = extractedChunkHeader.length();
         extractedChunkHeader = StringUtil.substring(extractedChunkHeader, firstSeparatorIdx + 1).trim();
         if (extractedChunkHeader.endsWith("}"))
         {
            extractedChunkHeader = StringUtil.substring(
                  extractedChunkHeader, 0, extractedChunkHeader.length() -1).trim();
         }
         return new ChunkLabelInfo(extractedChunkHeader, originalLength);
      }

      // find second separator (comma plus optional spaces) or end of string if none
      int secondSeparatorIdx = extractedChunkHeader.indexOf(',', firstSeparatorIdx + 1);
      if (secondSeparatorIdx == -1)
      {
         secondSeparatorIdx = extractedChunkHeader.length();
      }

      // determine if first token is a label (i.e. doesn't contain an equal sign)
      if (firstEqualsIdx < secondSeparatorIdx)
      {
         // ```{r foo=bar}
         return new ChunkLabelInfo("", firstSeparatorIdx);
      }

      // the text from the first separator to the next one is the label
      return new ChunkLabelInfo(
            StringUtil.substring(extractedChunkHeader, firstSeparatorIdx + 1, secondSeparatorIdx).trim(),
            secondSeparatorIdx);
   }

   // Public methods ----------------------------------------------------------

   public int getPreambleRow()
   {
      return getRow();
   }
   
   public void setState(int state)
   {
      toolbar_.setState(state);
   }
   
   public void syncToChunk()
   {
      int row = getRow();
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
      
      syncOptions();
   }
   
   public void syncOptions()
   {
      toolbar_.setShowOptions(true);
   }

   /**
    * Get the HTML element hosting the toolbar.
    *
    * @return An HTML element, or null if the toolbar hasn't been created yet
    */
   public Element getElement()
   {
      if (toolbar_ == null)
      {
         return null;
      }
      return toolbar_.getElement();
   }

   // ChunkContextToolbar.Host implementation ---------------------------------
   
   @Override
   public void runPreviousChunks()
   {
      outerEditor_.executeChunks(chunkPosition(), 
            TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
   }

   @Override
   public void runChunk()
   {
      outerEditor_.executeChunk(chunkPosition());
      outerEditor_.focus();
   }

   @Override
   public void showOptions(int x, int y)
   {
      ChunkOptionsPopupPanel panel = createPopupPanel();
      
      panel.init(innerEditor_, Position.create(getInnerRow(), 0));
      panel.show();
      panel.focus();
      PopupPositioner.setPopupPosition(panel, x, y, 10);
   }
   
   @Override
   public void interruptChunk()
   {
      outerEditor_.fireEvent(new InterruptChunkEvent(
            outerChunk_.getPreamble().getRow()));
   }

   @Override
   public void dequeueChunk()
   {
      RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            constants_.chunkPendingExecution(),
            constants_.chunkPendingExecutionMessage(),
            false, // include cancel
            null,  // yes operation,
            new Operation() 
            {
               @Override
               public void execute()
               {
                  outerEditor_.dequeueChunk(getRow());
               }
            }, 
            null,  // cancel operation 
            constants_.okFullyCapitalized(),
            constants_.dontRun(), true);
   }

   @Override
   public void switchChunk(String chunkType)
   {
      if (outerChunk_ != null)
      {
         DocDisplay docDisplay = outerEditor_.getDocDisplay();
         
         Position start = outerChunk_.getPreamble();
         Position end = outerChunk_.getEnd();
         
         String chunkText = docDisplay.getTextForRange(Range.fromPoints(start, end));
         JsArrayString chunkLines = StringUtil.split(chunkText, "\n");
         if (chunkLines.length() > 0)
         {
            String firstLine = chunkLines.get(0);
            Position linedEnd = Position.create(start.getRow(),firstLine.length());
            
            String newFirstLine = firstLine.replaceFirst("[, ]*engine='[a-zA-Z]+'", "");
            newFirstLine = newFirstLine.replaceFirst("{[a-zA-Z]+", "{" + chunkType);

            docDisplay.replaceRange(Range.fromPoints(start, linedEnd), newFirstLine);
            
            outerEditor_.getNotebook().clearChunkOutput(outerChunk_);
         }
      }
   }
   
   public void setScope(Scope scope)
   {
      outerChunk_ = scope;
   }
   
   public ChunkContextToolbar getToolbar()
   {
      return toolbar_;
   }

   // Protected methods -------------------------------------------------------

   protected void createToolbar(int row)
   {
      toolbar_ = new ChunkContextToolbar(this, dark_, !isSetup_, engine_);
      toolbar_.setHeight("0px"); 
      toolbar_.setClassId(getLabel(row));
   }

   protected OptionLocation preferredOptionLocation()
   {
      // quarto docs prefer YAML chunk options, i.e. "#| foo: bar"
      return outerEditor_.getExtendedFileType().equals(SourceDocument.XT_QUARTO_DOCUMENT) ?
         OptionLocation.Yaml : OptionLocation.FirstLine;
   }
    
   protected abstract int getRow();
   
   protected abstract int getInnerRow();

   // Private methods ---------------------------------------------------------
   
   private Position chunkPosition()
   {
      return Position.create(getRow(), 0);
   }
   
   private boolean isSetupChunk(int row)
   {
      String line = outerEditor_.getDocDisplay().getLine(row);
      return line.contains("r setup");
   }

   private String getEngine(int row)
   {
      return outerEditor_.getEngineForRow(row);
   
   }
   
   private String getLabel(int row)
   {
      String line = outerEditor_.getDocDisplay().getLine(row);
      return extractChunkLabel(line).label;
   }

   private ChunkOptionsPopupPanel createPopupPanel()
   {
      boolean isVisualEditor = outerEditor_.isVisualEditorActive();
      int row = getRow();
      if (isSetupChunk(row))
         return new SetupChunkOptionsPopupPanel(preferredOptionLocation(), isVisualEditor);
      
      String engine = getEngine(row);
      if (!engine.toLowerCase().equals("r") &&
          !engine.toLowerCase().equals("d3"))
         return new CustomEngineChunkOptionsPopupPanel(engine_, preferredOptionLocation(), isVisualEditor);
      
      return new DefaultChunkOptionsPopupPanel(engine_, preferredOptionLocation(), isVisualEditor);
   }

   protected ChunkContextToolbar toolbar_;
   protected final TextEditingTarget outerEditor_;
   protected final DocDisplay innerEditor_;
   protected Scope outerChunk_;

   private final boolean dark_;

   private boolean isSetup_;
   private String engine_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
