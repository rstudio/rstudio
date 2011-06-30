/*
 * UnifiedEmitter.java
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

import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class UnifiedEmitter
{
   public UnifiedEmitter(String relPath)
   {
      this("a/" + relPath , "b/" + relPath);
   }

   public UnifiedEmitter(String fileA, String fileB)
   {
      fileA_ = fileA;
      fileB_ = fileB;
   }

   public void addDiffs(DiffChunk chunk, ArrayList<Line> lines)
   {
      contextLines_.addAll(chunk.diffLines);
      diffLines_.addAll(lines);
   }

   public String createPatch()
   {
      final ArrayList<DiffChunk> chunks = toDiffChunks(generateOutputLines());

      if (chunks.size() == 0)
         return "";

      StringBuilder p = new StringBuilder();

      p.append("--- ").append(fileA_).append(EOL);
      p.append("+++ ").append(fileB_).append(EOL);

      for (DiffChunk chunk : chunks)
      {
         p.append("@@ -")
               .append(chunk.oldRowStart)
               .append(',')
               .append(chunk.oldRowCount)
               .append(" +")
               .append(chunk.newRowStart)
               .append(',')
               .append(chunk.newRowCount)
               .append(" @@")
               .append(EOL);

         for (Line line : chunk.diffLines)
         {
            switch (line.getType())
            {
               case Same:
                  p.append(' ');
                  break;
               case Insertion:
                  p.append('+');
                  break;
               case Deletion:
                  p.append('-');
                  break;
               default:
                  throw new IllegalArgumentException();
            }
            p.append(line.getText()).append(EOL);
         }
      }

      return p.toString();
   }

   private ArrayList<DiffChunk> toDiffChunks(ArrayList<Line> lines)
   {
      ArrayList<DiffChunk> chunks = new ArrayList<DiffChunk>();

      if (lines.size() == 0)
         return chunks;

      int line = lines.get(0).getOldLine();

      int head = 0;
      for (int i = 1; i < lines.size(); i++)
      {
         if ((lines.get(i).getOldLine() - line) > 1)
         {
            List<Line> sublist = lines.subList(head, i);
            chunks.add(contiguousLinesToChunk(sublist));
            head = i;
         }
         line = lines.get(i).getOldLine();
      }

      // Add final chunk
      List<Line> sublist = lines.subList(head, lines.size());
      chunks.add(contiguousLinesToChunk(sublist));

      return chunks;
   }

   private DiffChunk contiguousLinesToChunk(List<Line> sublist)
   {
      Line first = sublist.get(0);
      Line last = sublist.get(sublist.size() - 1);
      return new DiffChunk(first.getOldLine(),
                           last.getOldLine() - first.getOldLine(),
                           first.getNewLine(),
                           last.getNewLine() - first.getNewLine(),
                           "",
                           new ArrayList<Line>(sublist));
   }

   private ArrayList<Line> generateOutputLines()
   {
      for (int i = 0; i < contextLines_.size(); i++)
         if (contextLines_.get(i).getType() == Type.Insertion)
         {
            contextLines_.remove(i);
            i--;
         }
      for (int i = 0; i < diffLines_.size(); i++)
         if (diffLines_.get(i).getType() == Type.Same)
         {
            diffLines_.remove(i);
            i--;
         }

      if (diffLines_.size() == 0)
         return new ArrayList<Line>();

      Collections.sort(contextLines_);
      DuplicateHelper.dedupeSortedList(contextLines_);

      Collections.sort(diffLines_);
      DuplicateHelper.dedupeSortedList(diffLines_);

      final ArrayList<Line> output = new ArrayList<Line>();

      final Iterator<Line> ctxit = contextLines_.iterator();
      final Iterator<Line> dffit = diffLines_.iterator();
      Line ctx = ctxit.hasNext() ? ctxit.next() : null;
      Line dff = dffit.hasNext() ? dffit.next() : null;

      int skew = 0;
      int lastKnownOldLine = 0;

      // Do this while loop while both iterators still have elements
      while (ctx != null && dff != null)
      {
         int cmp = ctx.getOldLine() - dff.getOldLine();
         if (cmp < 0 || (cmp == 0 && !ctx.equals(dff)))
         {
            if (ctx.getOldLine() > lastKnownOldLine)
            {
               processContextLine(output, ctx, skew);
               lastKnownOldLine = ctx.getOldLine();
            }
            ctx = ctxit.hasNext() ? ctxit.next() : null;
         }
         else
         {
            skew = processDiffLine(output, dff, skew);
            lastKnownOldLine = dff.getOldLine();
            dff = dffit.hasNext() ? dffit.next() : null;
         }
      }

      // Finish off the context iterator if necessary
      while (ctx != null)
      {
         if (ctx.getOldLine() > lastKnownOldLine)
         {
            processContextLine(output, ctx, skew);
            lastKnownOldLine = ctx.getOldLine();
         }
         ctx = ctxit.hasNext() ? ctxit.next() : null;
      }

      // Finish off the diff iterator if necessary
      while (dff != null)
      {
         skew = processDiffLine(output, dff, skew);
         //lastKnownOldLine = dff.getOldLine(); // no longer necessary
         dff = dffit.hasNext() ? dffit.next() : null;
      }

      return output;
   }

   private void processContextLine(ArrayList<Line> output, Line ctx, int skew)
   {
      switch (ctx.getType())
      {
         case Same:
            output.add(new Line(Type.Same, ctx.getOldLine(),
                                ctx.getOldLine() + skew,
                                ctx.getText()));
            break;
         case Deletion:
            // This is a line that, in the source diff, was deleted from orig.
            // But since we're processing it as context, we ignore the delete,
            // so we turn it back into "Same".
            output.add(new Line(Type.Same, ctx.getOldLine(),
                                ctx.getOldLine() + skew,
                                ctx.getText()));
            break;
         case Insertion:
            // This is a line that, in the original diff, was inserted into
            // orig. But since we're processing it as context, we ignore the
            // insertion, and let the line drop on the floor.
            break;
      }
   }

   private int processDiffLine(ArrayList<Line> output, Line dff, int skew)
   {
      switch (dff.getType())
      {
         case Same:
            output.add(new Line(Type.Same, dff.getOldLine(),
                                dff.getOldLine() + skew,
                                dff.getText()));
            break;
         case Deletion:
            output.add(new Line(Type.Deletion, dff.getOldLine(),
                                dff.getOldLine() + skew,
                                dff.getText()));
            skew--;
            break;
         case Insertion:
            output.add(new Line(Type.Insertion, dff.getOldLine(),
                                dff.getOldLine() + skew,
                                dff.getText()));
            skew++;
      }
      return skew;
   }

   private final ArrayList<Line> contextLines_ = new ArrayList<Line>();
   private final ArrayList<Line> diffLines_ = new ArrayList<Line>();
   private final String fileA_;
   private final String fileB_;
   private static final String EOL = "\n";
}
