/*
 * DefaultChunkOptionsPopupPanel.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display;

import com.google.gwt.user.client.Command;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TextCursor;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi;

import java.util.HashMap;
import java.util.Map;

public class DefaultChunkOptionsPopupPanel extends ChunkOptionsPopupPanel
{
   public DefaultChunkOptionsPopupPanel(String engine)
   {
      super(true);
      
      engine_ = engine;
      enginePanel_.setVisible(false);
   }
   
   @Override
   protected void initOptions(Command afterInit)
   {
      originalLine_ = display_.getLine(position_.getRow());
      parseChunkHeader(originalLine_, originalChunkOptions_);
      for (Map.Entry<String, String> pair : originalChunkOptions_.entrySet())
         chunkOptions_.put(pair.getKey(), pair.getValue());

      if (engine_ == "r") printTableAsTextCb_.setVisible(true);

      afterInit.execute();
   }
   
   @Override
   protected void synchronize()
   {
      String modeId = display_.getModeId();
      Pair<String, String> chunkHeaderBounds = getChunkHeaderBounds(modeId);
      if (chunkHeaderBounds == null)
         return;
      
      String label = tbChunkLabel_.getText();
      String newLine =
            chunkHeaderBounds.first +
            chunkPreamble_;
      
      if (!label.isEmpty())
      {
         if (StringUtil.isNullOrEmpty(chunkPreamble_))
            newLine += label;
         else
            newLine += " " + label;
      }
      
      if (!chunkOptions_.isEmpty())
      {
         Map<String, String> sorted = sortedOptions(chunkOptions_);
         if (label.isEmpty())
            newLine += " ";
         else
            newLine += ", ";
         newLine += StringUtil.collapse(sorted, "=", ", ");
      }
      
      newLine +=
            chunkHeaderBounds.second +
            "\n";
      
      display_.replaceRange(
            Range.fromPoints(
                  Position.create(position_.getRow(), 0),
                  Position.create(position_.getRow() + 1, 0)), newLine);
   }
   
   @Override
   protected void revert()
   {
      if (position_ == null)
         return;
      
      Range replaceRange = Range.fromPoints(
            Position.create(position_.getRow(), 0),
            Position.create(position_.getRow() + 1, 0));
      
      display_.replaceRange(
            replaceRange,
            originalLine_ + "\n");
   }
   
   private Pair<String, String> getChunkHeaderBounds(String modeId)
   {
      if (modeId == "mode/rmarkdown")
         return new Pair<String, String>("```{", "}");
      else if (modeId == "mode/sweave")
         return new Pair<String, String>("<<", ">>=");
      else if (modeId == "mode/rhtml")
         return new Pair<String, String>("<!--", "");
      else if (modeId == "mode/c_cpp")
         return new Pair<String, String>("/***", "");
      else if (modeId == "mode/r")  // Used in visual mode for embedded chunk editor
         return new Pair<String, String>("{", "}");
      
      return null;
   }
   
   private String extractChunkPreamble(String extractedChunkHeader,
                                       String modeId)
   {
      if (modeId == "mode/sweave")
         return "";
      
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return extractedChunkHeader;
      
      int firstCommaIdx = extractedChunkHeader.indexOf(',');
      if (firstCommaIdx == -1)
         firstCommaIdx = extractedChunkHeader.length();
      
      String label = extractedChunkHeader.substring(
            0, Math.min(firstSpaceIdx, firstCommaIdx)).trim();
      
      return label;
   }
   
   private void parseChunkHeader(String line, HashMap<String, String> chunkOptions)
   {
      String modeId = display_.getModeId();
      
      Pattern pattern = null;
      if (modeId == "mode/rmarkdown")
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
      else if (modeId == "mode/sweave")
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
      else if (modeId == "mode/rhtml")
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
      else if (modeId == "mode/r")
         pattern = RegexUtil.RE_EMBEDDED_R_CHUNK_BEGIN;
      
      if (pattern == null) return;
      
      Match match = pattern.match(line,  0);
      if (match == null) return;
      
      String extracted = match.getGroup(1);
      chunkPreamble_ = extractChunkPreamble(extracted, modeId);
      
      String chunkLabel = ChunkContextUi.extractChunkLabel(extracted);
      if (!StringUtil.isNullOrEmpty(chunkLabel))
         tbChunkLabel_.setText(chunkLabel);
      
      // if we had a chunk label, then we want to navigate our cursor to
      // the first comma in the chunk header; otherwise, we start at the
      // first space. this is done to accept chunk headers of the form
      //
      //    ```{r message=FALSE}
      //
      // ie, those with no comma after the engine used
      int argsStartIdx = StringUtil.isNullOrEmpty(chunkLabel)
            ? extracted.indexOf(' ')
            : extracted.indexOf(',');
      
      String arguments = extracted.substring(argsStartIdx + 1);
      TextCursor cursor = new TextCursor(arguments);
      
      // consume commas and whitespace if needed
      cursor.consumeUntilRegex("[^\\s,]");
      
      int startIndex = 0;
      do
      {
         if (!cursor.fwdToCharacter('=', false))
            break;
         
         int equalsIndex = cursor.getIndex();
         int endIndex = arguments.length();
         if (cursor.fwdToCharacter(',', true) ||
             cursor.fwdToCharacter(' ', true))
         {
            endIndex = cursor.getIndex();
         }
         
         chunkOptions.put(
               arguments.substring(startIndex, equalsIndex).trim(),
               arguments.substring(equalsIndex + 1, endIndex).trim());
         
         startIndex = cursor.getIndex() + 1;
      }
      while (cursor.moveToNextCharacter());
   }
   
   private String engine_;
}
