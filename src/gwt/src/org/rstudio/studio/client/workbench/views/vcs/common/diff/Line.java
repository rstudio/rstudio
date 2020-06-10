/*
 * Line.java
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

import java.util.ArrayList;

public class Line implements Comparable<Line>
{
   public enum Type
   {
      Same(' '),
      Insertion('+'),
      Deletion('-'),
      Comment('\\'),
      Info('_');

      Type(char value)
      {
         value_ = value;
      }

      public char getValue()
      {
         return value_;
      }

      public Type getInverse()
      {
         switch (this)
         {
            case Same:
               return Same;
            case Insertion:
               return Deletion;
            case Deletion:
               return Insertion;
            case Comment:
               return Comment;
            case Info:
               return Info;
            default:
               assert false : "Couldn't getInverse on Type value";
               throw new IllegalStateException("Couldn't getInverse on Type value");
         }
      }

      private final char value_;
   }

   public Line(Type type, int oldLine, int newLine, String text, int diffIndex)
   {
      type_ = type;
      lines_ = new int[] {oldLine, newLine};
      appliesTo_ = new boolean[]{ true };
      text_ = text;
      diffIndex_ = diffIndex;
   }

   public Line(Type type,
               boolean[] appliesTo,
               int[] lines,
               String text,
               int diffIndex)
   {
      if (lines.length < 2)
         throw new IllegalArgumentException("Too few lines");
      if (appliesTo.length != lines.length)
         throw new IllegalArgumentException("appliesTo had unexpected length");

      type_ = type;
      appliesTo_ = appliesTo;
      lines_ = lines;
      text_ = text;
      diffIndex_ = diffIndex;
   }

   public Type getType()
   {
      return type_;
   }

   public int getOldLine()
   {
      return lines_[0];
   }

   public int getNewLine()
   {
      return lines_[1];
   }

   public String getText()
   {
      return text_;
   }

   public int getDiffIndex()
   {
      return diffIndex_;
   }

   public Line reverse()
   {
      if (appliesTo_.length > 2)
         throw new UnsupportedOperationException("Can't reverse combined diff");

      return new Line(type_.getInverse(),
                      lines_[1],
                      lines_[0],
                      text_,
                      diffIndex_);
   }

   @Override
   public int compareTo(Line line)
   {
      return diffIndex_ - line.diffIndex_;
   }

   @Override
   public int hashCode()
   {
      return diffIndex_;
   }

   @Override
   public boolean equals(Object o)
   {
      return o instanceof Line && compareTo((Line) o) == 0;
   }

   public static ArrayList<Line> reverseLines(ArrayList<Line> lines)
   {
      ArrayList<Line> rlines = new ArrayList<Line>(lines.size());
      for (Line line : lines)
         rlines.add(line.reverse());
      return rlines;
   }

   public int[] getLines()
   {
      return lines_;
   }

   public boolean[] getAppliesTo()
   {
      return appliesTo_;
   }

   private final Type type_;
   private final int[] lines_;
   private final boolean[] appliesTo_;
   private final String text_;
   private final int diffIndex_;
}
