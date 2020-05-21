/*
 * WordWrap.java
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
package org.rstudio.core.client;

public class WordWrap
{
   public WordWrap(int maxLineLength, boolean hardWrapIfNecessary)
   {
      maxLineLength_ = maxLineLength;
      hardWrapIfNecessary_ = hardWrapIfNecessary;
   }

   public void setWrappingEnabled(boolean wrappingEnabled)
   {
      wrappingEnabled_ = wrappingEnabled;
      if (!wrappingEnabled && !atBeginningOfLine())
         appendRaw("\n");
   }

   public void appendLine(String line)
   {
      if (!wrappingEnabled_)
      {
         int lastInsertionRow = row_;
         int lastInsertionPoint = lineLength_;
         appendRaw(line + "\n");
         onChunkWritten(line, lastInsertionRow, lastInsertionPoint, 0);
      }
      else
      {
         if (forceWrapBefore(line))
            if (!atBeginningOfLine())
               wrap();

         processLine(line);

         if (forceWrapAfter(line))
            appendRaw("\n");
      }
   }

   private boolean atBeginningOfLine()
   {
      // Don't need to worry about indentation here because indent isn't
      // written until some content is.
      return lineLength_ == 0;
   }

   public String getOutput()
   {
      return output_.toString();
   }

   public int getRow()
   {
      return row_;
   }

   private void processLine(String line)
   {
      assert line.indexOf('\n') < 0;

      String trimmed = StringUtil.trimLeft(line);
      int origStringPos = line.length() - trimmed.length();
      line = trimmed;
      line = StringUtil.trimRight(line);

      if (line.length() > 0 &&
          lineLength_ > 0 &&
          lineLength_ < maxLineLength_)
      {
         // We're about to append some content and we're not at the beginning
         // of the line.
         appendRaw(" ");
      }

      // Loop while "line" is too big to fit in the current line without
      // wrapping.
      
      // TODO: previously the following loop used 'while(true)' but would
      // hang on overly-large comment prefixes; e.g. 82 '#' in a row in an
      // R document. This is a stop-gap fix to ensure that we don't hang
      // when encountering such a situation.
      for (int i = 0; i < 1000; i++)
      {
         // chars left
         int charsLeft = lineLength_ == 0 ? maxLineLength_ - indent_.length()
                                          : maxLineLength_ - lineLength_;

         if (line.length() <= charsLeft)
            break;

         int breakChars = 1;

         // Look for the last space that will fit on the current line
         int index = line.lastIndexOf(' ', charsLeft);
         if (index == -1)
         {
            if (lineLength_ == 0)
            {
               index = line.indexOf(' ', charsLeft);
               if (index == -1)
                  index = line.length();

               if (hardWrapIfNecessary_ && index > charsLeft)
               {
                  index = charsLeft;
                  breakChars = 0;
               }
            }
         }

         int insertionRow = row_;
         int insertionPoint = lineLength_;
         String thisChunk = "";
         if (index > 0)
         {
            thisChunk = line.substring(0, index);
            appendRawWithIndent(thisChunk);
         }
         wrap();
         onChunkWritten(thisChunk, insertionRow, insertionPoint, origStringPos);

         int nextLineIndex = Math.min(line.length(), index + breakChars);
         origStringPos += nextLineIndex;
         line = line.substring(nextLineIndex);
         trimmed = StringUtil.trimLeft(line);
         origStringPos += line.length() - trimmed.length();
         line = trimmed;
      }

      // Now just append the rest of the line
      int lastInsertionRow = row_;
      int lastInsertionPoint = lineLength_;
      appendRawWithIndent(line);
      onChunkWritten(line, lastInsertionRow, lastInsertionPoint, origStringPos);
   }

   protected void onChunkWritten(String chunk,
                                 int insertionRow,
                                 int insertionCol,
                                 int indexInOriginalString)
   {

   }

   protected boolean forceWrapBefore(String line)
   {
      return isEmpty(line);
   }

   protected boolean forceWrapAfter(String line)
   {
      return isEmpty(line);
   }

   private boolean isEmpty(String line)
   {
      return line.trim().length() == 0;
   }

   private void wrap()
   {
      if (output_.length() > 0)
         appendRaw("\n");
   }

   private void appendRawWithIndent(String value)
   {
      assert value.indexOf('\n') < 0;
      if (lineLength_ == 0 && indent_ != null)
         appendRaw(indent_);
      appendRaw(value);
   }

   private void appendRaw(String value)
   {
      if (value.length() == 0)
         return;

      output_.append(value);
      int index = value.lastIndexOf('\n');
      if (index < 0)
         lineLength_ += value.length();
      else
         lineLength_ = value.length() - (index + 1);

      for (int i = 0; i < value.length(); i++)
         if (value.charAt(i) == '\n')
            row_++;
   }

   protected String indent_ = "";

   private final StringBuilder output_ = new StringBuilder();
   private int lineLength_;
   private int row_ = 0;
   private final int maxLineLength_;
   private final boolean hardWrapIfNecessary_;
   private boolean wrappingEnabled_ = true;
}
