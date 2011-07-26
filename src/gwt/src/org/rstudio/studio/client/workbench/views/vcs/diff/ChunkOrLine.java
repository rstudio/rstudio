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
