/*
 * UnifiedParser.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.diff;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;

import java.util.ArrayList;

public class UnifiedParser
{
   public UnifiedParser(String data)
   {
      data_ = data;
   }

   public DiffFileHeader nextFilePair()
   {
      ArrayList<String> headerLines = new ArrayList<String>();

      boolean inDiff = false;

      String line;
      while (null != (line = nextLine()) && !line.startsWith("--- "))
      {
         if (line.startsWith("diff "))
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

   public DiffChunk nextChunk()
   {
      String line;
      while (null != (line = nextLine()) && !(line.startsWith("@@ ") || line.startsWith("--- ")))
      {
      }

      if (line == null)
         return null;

      if (line.startsWith("--- "))
         return null;

      Match match = range_.match(line, 0);
      if (match == null)
         throw new DiffFormatException("Malformed chunk header");

      final int oldRowStart = Integer.parseInt(match.getGroup(1));
      final int oldCount = match.hasGroup(2) ? Integer.parseInt(match.getGroup(2)) : 1;
      final int newRowStart = Integer.parseInt(match.getGroup(3));
      final int newCount = match.hasGroup(4) ? Integer.parseInt(match.getGroup(4)) : 1;
      final String text = match.getGroup(6);

      int oldRow = oldRowStart;
      int oldRowsLeft = oldCount;
      int newRow = newRowStart;
      int newRowsLeft = newCount;

      ArrayList<Line> lines = new ArrayList<Line>();
      while (oldRowsLeft > 0 || newRowsLeft > 0 || nextLineIsComment())
      {
         String diffLine = nextLine();
         if (diffLine == null)
            throw new DiffFormatException("Diff ended prematurely");
         if (diffLine.length() == 0)
            throw new DiffFormatException("Unexpected blank line");
         switch (diffLine.charAt(0))
         {
            case ' ':
               oldRowsLeft--;
               newRowsLeft--;
               lines.add(new Line(Type.Same,
                                  oldRow++,
                                  newRow++,
                                  diffLine.substring(1)));
               break;
            case '-':
               oldRowsLeft--;
               lines.add(new Line(Type.Deletion,
                                  oldRow++,
                                  newRow-1,
                                  diffLine.substring(1)));
               break;
            case '+':
               newRowsLeft--;
               lines.add(new Line(Type.Insertion,
                                  oldRow-1,
                                  newRow++,
                                  diffLine.substring(1)));
               break;
            case '\\':
               // e.g. "\\ No newline at end of file"
               lines.add(new Line(Type.Comment,
                                  oldRow-1,
                                  newRow-1,
                                  diffLine.substring(1)));
               break;
            default:
               throw new DiffFormatException("Unexpected leading character");
         }

         if (oldRowsLeft < 0 || newRowsLeft < 0)
            throw new DiffFormatException("Diff ended prematurely");
      }

      return new DiffChunk(oldRowStart, oldCount, newRowStart, newCount,
                           text, lines);
   }

   private boolean isEOL()
   {
      return pos_ >= data_.length();
   }

   private boolean nextLineIsComment()
   {
      return !isEOL() && data_.charAt(pos_) == '\\';
   }

   private String nextLine()
   {
      if (isEOL())
         return null;

      Match match = newline_.match(data_, pos_);
      if (match == null)
      {
         int pos = pos_;
         pos_ = data_.length();
         return data_.substring(pos);
      }
      else
      {
         String value = data_.substring(pos_, match.getIndex());
         pos_ = match.getIndex() + match.getValue().length();
         return value;
      }
   }

   private final String data_;
   private int pos_;
   private final Pattern newline_ = Pattern.create("\\r?\\n");
   private final Pattern range_ = Pattern.create("^@@\\s*-([\\d]+)(?:,([\\d]+))?\\s+\\+([\\d]+)(?:,([\\d]+))?\\s*@@( (.*))?$", "m");
}
