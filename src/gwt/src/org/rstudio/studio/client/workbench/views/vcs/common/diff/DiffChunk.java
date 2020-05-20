/*
 * DiffChunk.java
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

public class DiffChunk
{
   public DiffChunk(Range[] ranges,
                    String lineText,
                    ArrayList<Line> diffLines,
                    int diffIndex)
   {
      this.ranges_ = ranges;
      this.lineText_ = lineText;
      this.diffLines_ = diffLines;
      diffIndex_ = diffIndex;
   }

   public DiffChunk reverse()
   {
      if (ranges_.length != 2)
         throw new UnsupportedOperationException(
               "Can't reverse a combined diff");

      Range[] newRanges = new Range[2];
      newRanges[0] = ranges_[1];
      newRanges[1] = ranges_[0];

      return new DiffChunk(newRanges,
                           lineText_,
                           Line.reverseLines(diffLines_), diffIndex_);
   }

   public ArrayList<Line> getLines()
   {
      return diffLines_;
   }

   public Range[] getRanges()
   {
      return ranges_;
   }

   public String getLineText()
   {
      return lineText_;
   }

   public int getDiffIndex()
   {
      return diffIndex_;
   }

   /**
    * Returns true if this is a chunk that can't be rendered. Needed for SVN
    * property chunks (these don't have a header but the DiffParser interface
    * requires lines to be returned in chunks).
    */
   public boolean shouldIgnore()
   {
      return ranges_ == null;
   }

   private final String lineText_;
   private final ArrayList<Line> diffLines_;
   private final int diffIndex_;
   private final Range[] ranges_;
}
