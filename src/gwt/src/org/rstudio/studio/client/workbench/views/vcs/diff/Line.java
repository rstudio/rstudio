/*
 * Line.java
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

import java.util.ArrayList;

public class Line implements Comparable<Line>
{
   public enum Type
   {
      Same(' '),
      Insertion('+'),
      Deletion('-'),
      Comment('\\');

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
            default:
               assert false : "Couldn't getInverse on Type value";
               throw new IllegalStateException("Couldn't getInverse on Type value");
         }
      }

      private final char value_;
   }

   public Line(Type type, int oldLine, int newLine, String text)
   {
      type_ = type;
      oldLine_ = oldLine;
      newLine_ = newLine;
      text_ = text;
   }

   public Type getType()
   {
      return type_;
   }

   public int getOldLine()
   {
      return oldLine_;
   }

   public int getNewLine()
   {
      return newLine_;
   }

   public String getText()
   {
      return text_;
   }

   public Line reverse()
   {
      return new Line(type_.getInverse(),
                      newLine_,
                      oldLine_,
                      text_);
   }

   @Override
   public int compareTo(Line line)
   {
      int comp = oldLine_ - line.oldLine_;
      if (comp == 0)
         comp = newLine_ - line.newLine_;
      if (comp == 0)
         comp = type_.getValue() - line.type_.getValue();
      return comp;
   }

   @Override
   public int hashCode()
   {
      return (type_.getValue() + "/" + oldLine_ + ":" + newLine_).hashCode();
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

   private final Type type_;
   private final int oldLine_;
   private final int newLine_;
   private final String text_;
}
