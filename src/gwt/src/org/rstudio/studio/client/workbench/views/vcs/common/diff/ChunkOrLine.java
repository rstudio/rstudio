/*
 * ChunkOrLine.java
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

public class ChunkOrLine
{
   public static ArrayList<ChunkOrLine> fromChunk(DiffChunk chunk)
   {
      ArrayList<ChunkOrLine> list = new ArrayList<ChunkOrLine>();
      if (!chunk.shouldIgnore())
         list.add(new ChunkOrLine(chunk));
      for (Line line : chunk.getLines())
         list.add(new ChunkOrLine(line));
      return list;
   }

   public ChunkOrLine(DiffChunk chunk)
   {
      chunk_ = chunk;
      line_ = null;
   }

   public ChunkOrLine(Line line)
   {
      line_ = line;
      chunk_ = null;
   }

   public DiffChunk getChunk()
   {
      return chunk_;
   }

   public Line getLine()
   {
      return line_;
   }

   private final DiffChunk chunk_;
   private final Line line_;
}
