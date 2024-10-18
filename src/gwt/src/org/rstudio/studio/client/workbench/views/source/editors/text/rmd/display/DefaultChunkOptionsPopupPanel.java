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

import org.rstudio.core.client.CommandWithArg;
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
   protected void initOptions(CommandWithArg<Boolean> afterInit)
   {
      // values used to restore via "Revert" button
      originalFirstLine_ = display_.getLine(position_.getRow());
      originalOptionLines_ = getRawYamlOptionLines(position_.getRow() + 1);

      // extract chunk options from first line, e.g. {r, echo=TRUE}
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      parseChunkHeader(originalFirstLine_, isVisualEditor_ ? "mode/r" : display_.getModeId(),
                       chunkOptions_, extraInfo);
      chunkPreamble_ = extraInfo.chunkPreamble;

      // found a label via {r LABEL} approach, store it to track where it came from
      if (!StringUtil.isNullOrEmpty(extraInfo.chunkLabel))
         set("label", extraInfo.chunkLabel, OptionLocation.FirstLine);

      // extract chunk options from YAML lines, e.g. "#| echo: true"
      if (!parseYamlChunkOptions(getYamlOptionLines(position_.getRow() + 1), chunkOptions_))
      {
         // if unable to parse YAML we block the editing UI and require user to edit manually
         afterInit.execute(false);
         return;
      }

      // set the label in the UI
      String label = getLabelValue();
      if (!StringUtil.isNullOrEmpty(label))
         tbChunkLabel_.setText(label);

      if (StringUtil.equals(engine_, "r"))
         printTableAsTextCb_.setVisible(true);

      afterInit.execute(true);
   }

   @Override
   protected void synchronize()
   {
      String label = tbChunkLabel_.getText();

      // determine where to write the label based on where we originally found it
      OptionLocation labelLocation = preferredOptionLocation_;
      ChunkOptionValue labelInfo = get("label");
      if (labelInfo != null)
         labelLocation = labelInfo.getLocation();

      // update label in the options map
      set("label", label, labelLocation);

      // persist
      if (!writeFirstLineOptions())
         return;
      writeYamlOptionLines();
   }

   /**
    * Write chunk options into the first line
    * @return true if successful, false if unable to proceed
    */
   private boolean writeFirstLineOptions()
   {
      String modeId = display_.getModeId();
      Pair<String, String> chunkHeaderBounds = getChunkHeaderBounds(modeId);
      if (chunkHeaderBounds == null)
         return false;

      String newLine = chunkHeaderBounds.first + chunkPreamble_;

      String label = getLabelForLocation(OptionLocation.FirstLine);
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
   private void writeYamlOptionLines()
   {
      // Compose replacement string
      String newLines = "";
      Map<String, String> sorted = sortedOptionsDenormalized(chunkOptions_, OptionLocation.Yaml);
      if (!sorted.isEmpty())
      {
         newLines = StringUtil.collapse(sorted, "#| ", ": ", "\n");
      }

      if (!newLines.isEmpty())
         newLines += "\n";

      deleteYamlOptions();
      display_.setCursorPosition(Position.create(position_.getRow() + 1, 0));
      display_.insertCode(newLines);
   }

   private void deleteYamlOptions()
   {
      int existingYamlLineCount = countYamlOptionLines(position_.getRow() + 1);
      Range deleteRange = Range.fromPoints(
            Position.create(position_.getRow() + 1, 0),
            Position.create(position_.getRow() + 1 + existingYamlLineCount, 0));
      display_.clearSelection();
      for (int row = deleteRange.getEnd().getRow() - 1; row >= deleteRange.getStart().getRow(); row--)
      {
         display_.setCursorPosition(Position.create(row, 0));
         display_.removeLine();
      }
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

      deleteYamlOptions();
      display_.setCursorPosition(Position.create(position_.getRow() + 1, 0));
      display_.insertCode(originalOptionLines_ + (originalOptionLines_.isEmpty() ? "" : "\n"));
   }

   private Pair<String, String> getChunkHeaderBounds(String modeId)
   {
      // When using Visual Editor the mode is the chunk's mode, not the document's
      // mode. Leverage fact the Visual Editor only supports markdown-like formats
      // using a modified form of the rmarkdown-style boundaries.
      if (isVisualEditor_)
         return new Pair<>("{", "}");
      else if (StringUtil.equals(modeId, "mode/rmarkdown"))
         return new Pair<>("```{", "}");
      else if (StringUtil.equals(modeId, "mode/sweave"))
         return new Pair<>("<<", ">>=");
      else if (StringUtil.equals(modeId, "mode/rhtml"))
         return new Pair<>("<!--", "");
      else if (StringUtil.equals(modeId, "mode/c_cpp"))
         return new Pair<>("/***", "");

      return null;
   }

   private static String extractChunkPreamble(String extractedChunkHeader,
                                              String modeId)
   {
      if (StringUtil.equals(modeId, "mode/sweave"))
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
      if (StringUtil.equals(modeId, "mode/rmarkdown"))
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
      else if (StringUtil.equals(modeId, "mode/sweave"))
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
      else if (StringUtil.equals(modeId, "mode/rhtml"))
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
      else if (StringUtil.equals(modeId, "mode/r"))
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
     * @return true if successfully parsed, false if not successfully parsed
     */
    public static boolean parseYamlChunkOptions(List<String> optionLines, Map<String, ChunkOptionValue> chunkOptions)
    {
      String yaml = String.join("\n", optionLines);
      Object parsedYaml = null;

      try
      {
         parsedYaml = Yaml.load(yaml);
      }
      catch(Exception e)
      {
         // unsupported YAML
         return false;
      }

      // flatten the yaml into "name": "value"
      JsMapString opts = yamlToStringMap(parsedYaml);
      
      // add/update chunk options
      JsArrayString keys = opts.keys();
      for (int i = 0; i < keys.length(); i++)
      {
         String key = keys.get(i);
         String value = opts.get(key).trim();
         key = ChunkOptionValue.normalizeOptionName(key);
         chunkOptions.put(key, new ChunkOptionValue(value, OptionLocation.Yaml));
      }
      return true;
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