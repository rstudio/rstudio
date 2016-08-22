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

import java.util.HashMap;
import java.util.Map;

public class DefaultChunkOptionsPopupPanel extends ChunkOptionsPopupPanel
{
   public DefaultChunkOptionsPopupPanel()
   {
      super(true);
      
      enginePanel_.setVisible(false);
   }
   
   @Override
   protected void initOptions(Command afterInit)
   {
      originalLine_ = display_.getLine(position_.getRow());
      parseChunkHeader(originalLine_, originalChunkOptions_);
      for (Map.Entry<String, String> pair : originalChunkOptions_.entrySet())
         chunkOptions_.put(pair.getKey(), pair.getValue());
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
         if (!(StringUtil.isNullOrEmpty(chunkPreamble_) &&
             label.isEmpty()))
         {
            newLine += ", ";
         }
         
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
      if (modeId.equals("mode/rmarkdown"))
         return new Pair<String, String>("```{", "}");
      else if (modeId.equals("mode/sweave"))
         return new Pair<String, String>("<<", ">>=");
      else if (modeId.equals("mode/rhtml"))
         return new Pair<String, String>("<!--", "");
      else if (modeId.equals("mode/c_cpp"))
         return new Pair<String, String>("/***", "");
      
      return null;
   }
   
   private String extractChunkPreamble(String extractedChunkHeader,
                                       String modeId)
   {
      if (modeId.equals("mode/sweave"))
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
   
   private String extractChunkLabel(String extractedChunkHeader)
   {
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return "";
      
      int firstCommaIdx = extractedChunkHeader.indexOf(',');
      if (firstCommaIdx == -1)
         firstCommaIdx = extractedChunkHeader.length();
      
      return firstCommaIdx <= firstSpaceIdx ?
            "" :
            extractedChunkHeader.substring(firstSpaceIdx + 1, firstCommaIdx).trim();
   }
   
   private void parseChunkHeader(String line, HashMap<String, String> chunkOptions)
   {
      String modeId = display_.getModeId();
      
      Pattern pattern = null;
      if (modeId.equals("mode/rmarkdown"))
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
      else if (modeId.equals("mode/sweave"))
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
      else if (modeId.equals("mode/rhtml"))
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
      
      if (pattern == null) return;
      
      Match match = pattern.match(line,  0);
      if (match == null) return;
      
      String extracted = match.getGroup(1);
      chunkPreamble_ = extractChunkPreamble(extracted, modeId);
      
      String chunkLabel = extractChunkLabel(extracted);
      if (!StringUtil.isNullOrEmpty(chunkLabel))
         tbChunkLabel_.setText(extractChunkLabel(extracted));
      
      int firstCommaIndex = extracted.indexOf(',');
      String arguments = extracted.substring(firstCommaIndex + 1);
      TextCursor cursor = new TextCursor(arguments);
      
      int startIndex = 0;
      do
      {
         if (!cursor.fwdToCharacter('=', false))
            break;
         
         int equalsIndex = cursor.getIndex();
         int endIndex = arguments.length();
         if (cursor.fwdToCharacter(',', true))
            endIndex = cursor.getIndex();
         
         chunkOptions.put(
               arguments.substring(startIndex, equalsIndex).trim(),
               arguments.substring(equalsIndex + 1, endIndex).trim());
         
         startIndex = cursor.getIndex() + 1;
      }
      while (cursor.moveToNextCharacter());
   }
   
   

}
