/*
 * WordWrapCursorTracker.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Point;

public class WordWrapCursorTracker
{
   public WordWrapCursorTracker(int row, int column)
   {
      row_ = row;
      column_ = column;
   }

   public Point getResult()
   {
      return output_;
   }

   public void onBeginInputRow()
   {
      currentInputRow_++;
   }

   public void onChunkWritten(String chunk,
                              int outputRow,
                              int outputColumn,
                              int inputColumn)
   {
      if (output_ != null)
         return; // we already have an answer

//      Debug.devlogf("Row: {1}, Chunk: {0}", chunk, row_);

      // Compare the current insertion point to the desired cursor position.
      int compare = currentInputRow_ - row_;
      if (compare == 0)
         compare = (inputColumn + chunk.length()) - column_;

      if (compare < 0)
      {
//         Debug.devlog("skip");
         // We haven't gotten there yet--do nothing
      }
      else if (currentInputRow_ == row_ && inputColumn <= column_)
      {
         // Cursor position is inside the current chunk--nice!
         output_ = Point.create(
               outputColumn + (column_ - inputColumn),
               outputRow);
//         Debug.devlogf("exact: {0}, {1}", output_.getY(), output_.getX());
      }
      else
      {
//         Debug.devlog("slop");
         // We've gone past the cursor position; use the current insertion
         // point before we get any further away
         output_ = Point.create(outputColumn, outputRow);
      }
   }


   private final int row_;
   private final int column_;

   private Point output_;

   private int currentInputRow_ = -1;
}
