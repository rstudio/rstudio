/*
 * UnifiedParser.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line.Type;

import java.util.ArrayList;

public class UnifiedParser implements DiffParser
{
   public UnifiedParser(String data)
   {
      this(data, 0);
   }

   public UnifiedParser(String data, int startDiffIndex)
   {
      data_ = data;
      diffIndex_ = startDiffIndex;
   }

   public int getDiffIndex()
   {
      return diffIndex_;
   }

   @Override
   public DiffFileHeader nextFilePair()
   {
      ArrayList<String> headerLines = new ArrayList<String>();

      boolean inDiff = false;

      String line;
      while (null != (line = nextLine()) && !line.startsWith("--- "))
      {
         if (isNewFileLine(line))
            inDiff = true;

         if (inDiff)
            headerLines.add(line);
      }

      if (line == null)
         return null;

      String fileA = line.substring(4);
      line = nextLine();
      if (line == null || !line.startsWith("+++ "))
         throw new DiffFormatException("Incomplete file header");
      String fileB = line.substring(4);
      return new DiffFileHeader(headerLines, fileA, fileB);
   }

   @Override
   public DiffChunk nextChunk()
   {
      String nextLine = peekLine();
      if (nextLine != null && isNewFileLine(nextLine))
         return null;

      String line;
      while (null != (line = nextLine()) && !(line.startsWith("@@") || line.startsWith("--- ")))
      {
      }

      if (line == null)
         return null;

      if (line.startsWith("--- "))
         return null;


      ChunkHeaderInfo chunkHeaderInfo = new ChunkHeaderParser(line).parse();
      if (chunkHeaderInfo == null)
         throw new DiffFormatException("Malformed chunk header");

      int chunkDiffIndex = diffIndex_++;

      Range[] ranges = chunkHeaderInfo.ranges;
      int[] counts = new int[ranges.length];
      int[] positions = new int[ranges.length];
      boolean[] MASK_NONE = new boolean[ranges.length];
      boolean[] MASK_ALL = new boolean[ranges.length];
      for (int i = 0; i < ranges.length; i++)
      {
         counts[i] = ranges[i].rowCount;
         positions[i] = ranges[i].startRow-1;
         MASK_ALL[i] = true;
      }
      int columns = ranges.length - 1;

      boolean[] mask = new boolean[ranges.length];

      ArrayList<Line> lines = new ArrayList<Line>();
      for (;
           !isEmpty(counts) || nextLineIsComment();
           diffIndex_++)
      {
         String diffLine = nextLine();
         if (diffLine == null)
            throw new DiffFormatException("Diff ended prematurely");
         if (diffLine.length() < columns)
            throw new DiffFormatException("Unexpected line format");

         int directive = ' ';
         for (int i = 0; i < columns; i++)
         {
            mask[i] = StringUtil.charAt(diffLine, i) != ' ';
            if (mask[i])
            {
               if (directive == ' ')
                  directive = StringUtil.charAt(diffLine, i);
               else if (directive != StringUtil.charAt(diffLine,i))
                  throw new DiffFormatException("Conflicting directives");
            }
         }

         switch (directive)
         {
            case ' ':
               // All positions increase by one (including new)

               addToSelected(positions, MASK_ALL, +1);
               addToSelected(counts, MASK_ALL, -1);
               lines.add(new Line(Type.Same,
                                  MASK_ALL,
                                  clone(positions),
                                  diffLine.substring(columns),
                                  diffIndex_));
               break;
            case '-':
               // Masked positions increase by one

               addToSelected(positions, mask, +1);
               addToSelected(counts, mask, -1);
               lines.add(new Line(Type.Deletion,
                                  clone(mask),
                                  clone(positions),
                                  diffLine.substring(columns),
                                  diffIndex_));
               break;
            case '+':
               // Unmasked positions increase by one (including new)

               addToUnselected(positions, mask, +1);
               addToUnselected(counts, mask, -1);
               lines.add(new Line(Type.Insertion,
                                  complement(mask),
                                  clone(positions),
                                  diffLine.substring(columns),
                                  diffIndex_));
               break;
            case '\\':
               // No positions move??

               // e.g. "\\ No newline at end of file"
               lines.add(new Line(Type.Comment,
                                  MASK_NONE,
                                  clone(positions),
                                  diffLine.substring(columns),
                                  diffIndex_));
               break;
            default:
               throw new DiffFormatException("Unexpected leading character");
         }
      }

      if (!isZero(counts))
         throw new DiffFormatException("Diff didn't match header ranges");

      return new DiffChunk(ranges, chunkHeaderInfo.extraInfo, lines, chunkDiffIndex);
   }

   private boolean isNewFileLine(String nextLine)
   {
      return nextLine.startsWith("diff ") || nextLine.startsWith("Index: ");
   }

   private boolean[] complement(boolean[] array)
   {
      boolean[] newArray = new boolean[array.length];
      for (int i = 0; i < array.length; i++)
         newArray[i] = !array[i];
      return newArray;
   }

   private int[] clone(int[] array)
   {
      int[] newArray = new int[array.length];
      System.arraycopy(array, 0, newArray, 0, array.length);
      return newArray;
   }

   private boolean[] clone(boolean[] array)
   {
      boolean[] newArray = new boolean[array.length];
      System.arraycopy(array, 0, newArray, 0, array.length);
      return newArray;
   }

   private void addToSelected(int[] array, boolean[] mask, int value)
   {
      for (int i = 0; i < mask.length; i++)
      {
         if (mask[i])
            array[i] += value;
      }
   }

   private void addToUnselected(int[] array, boolean[] mask, int value)
   {
      for (int i = 0; i < mask.length; i++)
      {
         if (!mask[i])
            array[i] += value;
      }
   }

   private boolean isEmpty(int[] array)
   {
      for (int i : array)
      {
         if (i > 0)
            return false;
      }
      return true;
   }

   private boolean isZero(int[] array)
   {
      for (int i : array)
      {
         if (i != 0)
            return false;
      }
      return true;
   }

   private boolean isEOD()
   {
      return pos_ >= data_.length();
   }

   private boolean nextLineIsComment()
   {
      return !isEOD() && StringUtil.charAt(data_, pos_) == '\\';
   }

   private String peekLine()
   {
      return nextLine(true);
   }

   private String nextLine()
   {
      return nextLine(false);
   }

   private String nextLine(boolean peek)
   {
      if (isEOD())
         return null;

      int head = pos_;
      // i will point to the tail (exclusive) of the string to be returned
      int i = data_.indexOf('\n', head);
      // length will indicate how far past i we should set pos_ to (unless peek)
      int length;

      if (i == -1)
      {
         i = data_.length();
         length = 0;
      }
      else if (i > 0 && StringUtil.charAt(data_, i - 1) == '\r')
      {
         i--;
         length = 2;
      }
      else
      {
         length = 1;
      }

      if (!peek)
         pos_ = i + length;

      return data_.substring(head, i);
   }

   private final String data_;
   private int pos_;
   private int diffIndex_;
}


class ChunkHeaderInfo
{
   ChunkHeaderInfo(Range[] ranges,
                   String extraInfo)
   {
      this.ranges = ranges;
      this.extraInfo = extraInfo;
   }

   public Range[] ranges;
   public String extraInfo;
}
