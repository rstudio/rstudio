/*
 * InputEditorSelection.java
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

public final class InputEditorSelection
      implements Comparable<InputEditorSelection>
{
   public InputEditorSelection(InputEditorPosition start,
                               InputEditorPosition end)
   {
      start_ = start;
      end_ = end;
   }

   public InputEditorSelection(InputEditorPosition at)
   {
      start_ = at;
      end_ = at;
   }

   public InputEditorPosition getStart()
   {
      return start_;
   }

   public InputEditorPosition getEnd()
   {
      return end_;
   }

   public int compareTo(InputEditorSelection o)
   {
      if (o == null)
         return 1;

      int result = getStart().compareTo(o.getStart());
      if (result == 0)
         result = getEnd().compareTo(o.getEnd());
      return result;
   }

   public boolean isEmpty()
   {
      return start_.equals(end_);
   }

   public InputEditorSelection extendToLineStart()
   {
      return new InputEditorSelection(
            start_.movePosition(0, false),
            end_);
   }

   public InputEditorSelection extendToLineEnd()
   {
      return new InputEditorSelection(
            start_,
            end_.movePosition(end_.getLineLength(), false));
   }

   @Override
   public String toString()
   {
      return "Start: " + (start_ == null ? "null" : start_) +
             ", End: " + (end_ == null ? "null" : end_);
   }

   public InputEditorSelection shrinkToNonEmptyLines()
   {
      InputEditorPosition newEnd = end_.skipEmptyLines(true, start_);
      if (newEnd == null || newEnd.compareTo(start_) <= 0)
         return new InputEditorSelection(start_, start_);
      InputEditorPosition newStart = start_.skipEmptyLines(false, end_);
      assert newStart != null;
      return new InputEditorSelection(newStart, newEnd);
   }

   public InputEditorSelection growToIncludeLines(String pattern)
   {
      return new InputEditorSelection(
            start_.growToIncludeLines(pattern, true),
            end_.growToIncludeLines(pattern, false));
   }

   private final InputEditorPosition start_;
   private final InputEditorPosition end_;
}
