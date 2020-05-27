/*
 * InputEditorPosition.java
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
package org.rstudio.studio.client.workbench.views.console.shell.editor;

public abstract class InputEditorPosition implements Comparable<InputEditorPosition>
{
   public InputEditorPosition(Object line, int position)
   {
      line_ = line;
      position_ = position;
   }
   
   public Object getLine()
   {
      return line_;
   }

   public int getPosition()
   {
      return position_;
   }

   public int compareTo(InputEditorPosition o)
   {
      if (o == null)
         return 1;

      int result = compareLineTo(o.getLine());
      if (result == 0)
         result = getPosition() - o.getPosition();

      return result;
   }

   @Override
   public int hashCode()
   {
      int hashCode = 0;
      if (line_ != null)
         hashCode = line_.hashCode() * 31;
      hashCode += position_;
      return hashCode;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      if (!(obj instanceof InputEditorPosition))
         return false;
      return compareTo((InputEditorPosition) obj) == 0;
   }

   @Override
   public String toString()
   {
      return "Position " + getPosition();
   }

   protected abstract int compareLineTo(Object other);

   public abstract InputEditorPosition movePosition(
         int position, boolean relative);
   
   public abstract InputEditorPosition moveToNextLine();
   public abstract InputEditorPosition moveToPreviousLine();

   public abstract int getLineLength();

   public abstract InputEditorPosition skipEmptyLines(
         boolean upwards,
         InputEditorPosition boundary);

   public abstract InputEditorPosition growToIncludeLines(String pattern,
                                                          boolean upwards);

   private final Object line_;
   private final int position_;
}
