/*
 * DefaultChunkOptionsPopupPanel.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TextCursor;
import org.rstudio.core.client.js.JsMapString;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.yaml.Yaml;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi.ChunkLabelInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue.OptionLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultChunkOptionsPopupPanel extends ChunkOptionsPopupPanel
{
   public DefaultChunkOptionsPopupPanel(String engine, OptionLocation optionLocation, boolean isVisualEditor)
   {
      super(true, optionLocation, isVisualEditor);

      engine_ = engine;
      enginePanel_.setVisible(false);
   }

   @Override
   protected void initOptions(Command afterInit)
   {
      // values used to restore via "Revert" button
      originalFirstLine_ = display_.getLine(position_.getRow());
      originalOptionLines_ = getRawYamlOptionLines(position_.getRow() + 1);

      // extract chunk options from first line, e.g. {r, echo=TRUE}
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      parseChunkHeader(originalFirstLine_, isVisualEditor_ ? "mode/r" : display_.getModeId(),
                       chunkOptions_, extraInfo);
      chunkPreamble_ = extraInfo.chunkPreamble;

      // extract chunk options from YAML lines, e.g. "#| echo: true"
      parseYamlChunkOptions(getYamlOptionLines(position_.getRow() + 1), chunkOptions_);

      if (!StringUtil.isNullOrEmpty(extraInfo.chunkLabel))
         tbChunkLabel_.setText(extraInfo.chunkLabel);

      if (engine_ == "r")
         printTableAsTextCb_.setVisible(true);

      afterInit.execute();
   }

   @Override
   protected void synchronize()
   {
      // TODO: write label to correct location (first line or YAML)
      String label = tbChunkLabel_.getText();

      if (!writeFirstLineOptions(label))
         return;

      writeYamlOptionLines(label);

   }

   /**
    * Write chunk options into the first line
    * @param label
    * @return true if successful, false if unable to proceed
    */
   private boolean writeFirstLineOptions(String label)
   {
      String modeId = display_.getModeId();
      Pair<String, String> chunkHeaderBounds = getChunkHeaderBounds(modeId);
      if (chunkHeaderBounds == null)
         return false;

      String newLine = chunkHeaderBounds.first + chunkPreamble_;

      if (!label.isEmpty())
      {
         if (StringUtil.isNullOrEmpty(chunkPreamble_))
            newLine += label;
         else
            newLine += " " + label;
      }

      Map<String, String> sorted = sortedOptionsDenormalized(chunkOptions_, OptionLocation.FirstLine);
      if (!sorted.isEmpty())
      {
         if (label.isEmpty())
            newLine += " ";
         else
            newLine += ", ";
         newLine += StringUtil.collapse(sorted, "=", ", ");
      }

      newLine += chunkHeaderBounds.second + "\n";

      display_.replaceRange(
            Range.fromPoints(
                  Position.create(position_.getRow(), 0),
                  Position.create(position_.getRow() + 1, 0)), newLine);

      return true;
   }

   /**
    * Write YAML line options
    */
   private void writeYamlOptionLines(String label)
   {
      // TODO: deal with label

      String newLines = "";
      Map<String, String> sorted = sortedOptionsDenormalized(chunkOptions_, OptionLocation.Yaml);
      if (!sorted.isEmpty())
         newLines = StringUtil.collapse(sorted, "#| ", ": ", "\n");
      if (!newLines.isEmpty())
         newLines += "\n";

      int existingYamlLineCount = countYamlOptionLines(position_.getRow() + 1);
      Range replaceRange = Range.fromPoints(
            Position.create(position_.getRow() + 1, 0),
            Position.create(position_.getRow() + 1 + existingYamlLineCount, 0));
      display_.replaceRange(replaceRange, newLines);
   }

   @Override
   protected void revert()
   {
      if (position_ == null)
         return;

      Range replaceRange = Range.fromPoints(
            Position.create(position_.getRow(), 0),
            Position.create(position_.getRow() + 1, 0));

      display_.replaceRange(replaceRange, originalFirstLine_ + "\n");

      int existingYamlLineCount = countYamlOptionLines(position_.getRow() + 1);
      replaceRange = Range.fromPoints(
            Position.create(position_.getRow() + 1, 0),
            Position.create(position_.getRow() + 1 + existingYamlLineCount, 0));

      display_.replaceRange(replaceRange,
                            originalOptionLines_ + (originalOptionLines_.isEmpty() ? "" : "\n"));
   }

   private Pair<String, String> getChunkHeaderBounds(String modeId)
   {
      // When using Visual Editor the mode is the chunk's mode, not the document's
      // mode. Leverage fact the Visual Editor only supports markdown-like formats
      // using a modified form of the rmarkdown-style boundaries.
      if (isVisualEditor_)
         return new Pair<>("{", "}");
      else if (modeId == "mode/rmarkdown")
         return new Pair<>("```{", "}");
      else if (modeId == "mode/sweave")
         return new Pair<>("<<", ">>=");
      else if (modeId == "mode/rhtml")
         return new Pair<>("<!--", "");
      else if (modeId == "mode/c_cpp")
         return new Pair<>("/***", "");

      return null;
   }

   private static String extractChunkPreamble(String extractedChunkHeader,
                                              String modeId)
   {
      if (modeId == "mode/sweave")
         return "";

      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      int firstCommaIdx = extractedChunkHeader.indexOf(',');

      if (firstSpaceIdx == -1 && firstCommaIdx == -1)
      {
         // entire string is the preamble, e.g. "{r}"
         return extractedChunkHeader;
      }

      if (firstCommaIdx == -1)
      {
         // {r foo}
         firstCommaIdx = firstSpaceIdx;
      }
      else if (firstSpaceIdx == -1)
      {
         // {r,foo}
         firstSpaceIdx = firstCommaIdx;
      }

      String label = StringUtil.substring(extractedChunkHeader,
            0, Math.min(firstSpaceIdx, firstCommaIdx)).trim();

      return label;
   }

   /**
    * Supplemental information returned by parseChunkHeader()
    */
   public static class ChunkHeaderInfo
   {
      public String chunkPreamble; // "r", "python", etc.
      public String chunkLabel; // label in header without label=, e.g. "foo" in {r foo}
   }

   /**
    * Parses chunk header (the first line of the chunk).
    *
    * TODO: Consider consolidating parseChunkHeader() and the RChunkHeaderParser class.
    */
   public static void parseChunkHeader(String line,
                                       String modeId,
                                       HashMap<String, ChunkOptionValue> chunkOptions,
                                       ChunkHeaderInfo extraInfo)
   {
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

      Match match = pattern.match(line, 0);
      if (match == null) return;

      String extracted = match.getGroup(1);
      extraInfo.chunkPreamble = extractChunkPreamble(extracted, modeId);

      ChunkLabelInfo labelDetails = ChunkContextUi.extractChunkLabel(extracted);
      extraInfo.chunkLabel = labelDetails.label;

      // continue parsing after the label
      int argsStartIdx = labelDetails.nextSepIndex;
      String arguments = StringUtil.substring(extracted, argsStartIdx + 1).trim();
      TextCursor cursor = new TextCursor(arguments);

      // consume commas and whitespace if needed
      cursor.consumeUntilRegex("[^\\s,]");

      int startIndex = 0;
      do
      {
         if (!cursor.fwdToNonQuotedCharacter('='))
            break;

         int equalsIndex = cursor.getIndex();
         int endIndex = arguments.length();
         if (cursor.fwdToCharacter(',', true))
         {
            endIndex = cursor.getIndex();
         }

         String optionName = StringUtil.substring(arguments, startIndex, equalsIndex).trim();
         chunkOptions.put(
               ChunkOptionValue.normalizeOptionName(optionName),
               new ChunkOptionValue(
                  StringUtil.substring(arguments, equalsIndex + 1, endIndex).trim(),
                  OptionLocation.FirstLine));

         startIndex = cursor.getIndex() + 1;
      }
      while (cursor.moveToNextCharacter());
   }

   /**
    * Returns list of option lines from the editor; these follow the first line,
    * must begin with "#| ", and must have at least one non-whitespace character
    * after "#| ". The lines aren't checked to see if they contain valid YAML, and
    * the option names are not normalized (e.g. fig.cap vs. fig-cap).
    * 
    * @param startLine first potential option line
    * @return list of option lines without the leading #|
    */
   private List<String> getYamlOptionLines(int startLine)
   {
      List<String> optionLines = new ArrayList<>();
      int currentLine = startLine;
      String line;
      while ((line = display_.getLine(currentLine)) != null)
      {
         // a line with no characters after the starting #| isn't valid and ends detection
         if (line.startsWith("#| ") && line.trim().length() > 2)
         {
            optionLines.add(line.substring("#| ".length()));
            currentLine++;
         }
         else
         {
            break;
         }
      }
      return optionLines;
   }

   /**
    * Returns count of option lines in the editor; these follow the first line and
    * must begin with "#|". No validation of line contents after the prefix is performed.
    * 
    * @param startLine first potential option line
    * @return number of lines found
    */
   private int countYamlOptionLines(int startLine)
   {
      int currentLine = startLine;
      String line;
      while ((line = display_.getLine(currentLine)) != null)
      {
         if (line.startsWith("#|"))
            currentLine++;
         else
            break;
      }
      return currentLine - startLine;
   }

   /**
    * Returns option lines from the editor in a single string, separated by "\n". No validation is
    * performed beyond checking the prefix "#|".
    * 
    * @param startLine first potential option line
    * @return "\n"-separated option lines
    */
   private String getRawYamlOptionLines(int startLine)
   {
      String optionLines = "";
      int currentLine = startLine;
      String line;
      while ((line = display_.getLine(currentLine)) != null)
      {
         if (line.startsWith("#|"))
         {
            if (!optionLines.isEmpty())
               optionLines = optionLines + "\n";
            optionLines = optionLines + line;
            currentLine++;
         }
         else
         {
            break;
         }
      }
      return optionLines;
   }

    /**
     * Parses YAML chunk options from the given lines and adds them to the collection.
     *
     * @param optionLines The lines containing raw YAML chunk options.
     * @param chunkOptions The map to populate with parsed options.
     */
    public static void parseYamlChunkOptions(List<String> optionLines, Map<String, ChunkOptionValue> chunkOptions)
    {
      String yaml = String.join("\n", optionLines);
      Object parsedYaml = null;

      try
      {
         parsedYaml = Yaml.load(yaml);
      }
      catch(Exception e)
      {
         // invalid yaml
         return;
      }

      // flatten the yaml into "name": "value"
      JsMapString opts = yamlToStringMap(parsedYaml);
      
      // add/update chunk options
      JsArrayString keys = opts.keys();
      for (int i = 0; i < keys.length(); i++)
      {
         String key = keys.get(i);
         String value = opts.get(key);
         key = ChunkOptionValue.normalizeOptionName(key);
         chunkOptions.put(key, new ChunkOptionValue(value, OptionLocation.Yaml));
      }
   }

   /**
    * Flatten parsed YAML into a flat map<string,string>. YAML supports nesting but
    * that's not needed here (AFAIK).
    *
    * @param yamlObject
    * @return
    */
   private static final native JsMapString yamlToStringMap(Object yamlObject)
   /*-{
      var result = {};
      for (var key in yamlObject) {
         if (yamlObject.hasOwnProperty(key)) {
            result[key] = String(yamlObject[key]);
         }
      }
      return result;
   }-*/;

   private String engine_;
}