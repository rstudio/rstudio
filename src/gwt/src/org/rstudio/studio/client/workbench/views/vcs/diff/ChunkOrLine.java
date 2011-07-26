/*
 * ChunkOrLine.java
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

public class ChunkOrLine
{
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
