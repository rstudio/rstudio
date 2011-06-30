/*
 * DiffChunk.java
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

public class DiffChunk
{
   public DiffChunk(int oldRowStart,
                    int oldRowCount,
                    int newRowStart,
                    int newRowCount,
                    String lineText,
                    ArrayList<Line> diffLines)
   {
      this.oldRowStart = oldRowStart;
      this.oldRowCount = oldRowCount;
      this.newRowStart = newRowStart;
      this.newRowCount = newRowCount;
      this.lineText = lineText;
      this.diffLines = diffLines;
   }

   public DiffChunk reverse()
   {
      return new DiffChunk(newRowStart, newRowCount,
                           oldRowStart, oldRowCount,
                           lineText,
                           Line.reverseLines(diffLines));
   }

   public final int oldRowStart;
   public final int oldRowCount;
   public final int newRowStart;
   public final int newRowCount;
   public final String lineText;
   public final ArrayList<Line> diffLines;
}
