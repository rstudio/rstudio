/*
 * RChunkHeaderParser.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.assist;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TextCursor;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class RChunkHeaderParser
{
   public static Map<String, String> parse(String header)
   {
      Map<String, String> options = new HashMap<String, String>();
      
      // determine an appropriate pattern for extracting options from
      // this header (infer based on the line contents)
      Pattern pattern = null;
      if (RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN.test(header))
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
      else if (RegexUtil.RE_SWEAVE_CHUNK_BEGIN.test(header))
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
      else if (RegexUtil.RE_RHTML_CHUNK_BEGIN.test(header))
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
      
      if (pattern == null)
         return options;
      
      Match match = pattern.match(header,  0);
      if (match == null)
         return options;
      
      String extracted = match.getGroup(1);
      String chunkLabel = extractChunkLabel(extracted);
      if (!StringUtil.isNullOrEmpty(chunkLabel))
         options.put("label", chunkLabel);
      
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
         
         options.put(
               arguments.substring(startIndex, equalsIndex).trim(),
               arguments.substring(equalsIndex + 1, endIndex).trim());
         
         startIndex = cursor.getIndex() + 1;
      }
      while (cursor.moveToNextCharacter());
      
      
      return options;
   }
   
   private static String extractChunkLabel(String extractedChunkHeader)
   {
      // if there are no spaces within the chunk header,
      // there cannot be a label. this implies a header of the form
      //
      //    ```{r}
      //
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return "";
      
      // find the indices of the first '=' and ',' characters
      int firstEqualsIdx = extractedChunkHeader.indexOf('=');
      int firstCommaIdx  = extractedChunkHeader.indexOf(',');
      
      // if we found neither an '=' nor a ',', then the label
      // must be all the text following the first space. this implies
      // a layout of the form:
      //
      //    ```{r label}
      //
      if (firstEqualsIdx == -1 && firstCommaIdx == -1)
         return extractedChunkHeader.substring(firstSpaceIdx + 1).trim();
      
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
      //
      //    ```{r label, message=TRUE}
      //
      return extractedChunkHeader.substring(firstSpaceIdx + 1, firstCommaIdx).trim();
   }
}
