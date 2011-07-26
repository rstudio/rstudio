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

/**
 * This class is used to subset an existing patch (the existing patch is
 * modeled in DiffChunk and the subset of changes we want to keep is zero
 * or more ArrayList&lt;Line&gt;).
 *
 * It works by using the existing patch to recreate the original data, then
 * merging with the specific changes we want to keep.
 *
 * The output is in "Unified diff" format.
 *
 * You can also use this class to generate reverse selective patches (basically
 * the same as above, but with the effect of undoing the patch on the changed
 * file, rather than applying the patch to the original file) by simply
 * reversing the DiffChunk (DiffChunk.reverse()) and lines (Line.reverseLines())
 * before calling addDiffs().
 */
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

   public void addContext(DiffChunk chunk)
   {
      contextLines_.addAll(chunk.diffLines);
   }

   public void addDiffs(ArrayList<Line> lines)
   {
      diffLines_.addAll(lines);
   }

   public String createPatch()
   {
      final ArrayList<DiffChunk> chunks = toDiffChunks(generateOutputLines());

      if (chunks.size() == 0)
         return "";

      StringBuilder p = new StringBuilder();

      // Write file header
      p.append("--- ").append(fileA_).append(EOL);
      p.append("+++ ").append(fileB_).append(EOL);

      for (DiffChunk chunk : chunks)
      {
         p.append(createChunkString(chunk));

         p.append(EOL);

         for (Line line : chunk.diffLines)
         {
            switch (line.getType())
            {
               case Same:       p.append(' '); break;
               case Insertion:  p.append('+'); break;
               case Deletion:   p.append('-'); break;
               default:
                  throw new IllegalArgumentException();
            }
            p.append(line.getText()).append(EOL);
         }
      }

      return p.toString();
   }

   public static String createChunkString(DiffChunk chunk)
   {
      StringBuilder sb = new StringBuilder();
      // Write chunk header: @@ -A,B +C,D @@
      sb.append("@@ -");
      sb.append(chunk.oldRowStart);
      sb.append(',');
      sb.append(chunk.oldRowCount);
      sb.append(" +");
      sb.append(chunk.newRowStart);
      sb.append(',');
      sb.append(chunk.newRowCount);
      sb.append(" @@");
      return sb.toString();
   }

   /**
    * Divide a list of sorted lines into DiffChunks.
    *
    * NOTE: If we cared about compact diffs we could detect long runs of
    * unchanged lines and elide them, like diff tools usually do. (Currently
    * we keep all the lines we're given, and only use discontinuities to
    * break up into chunks.)
    */
   private ArrayList<DiffChunk> toDiffChunks(ArrayList<Line> lines)
   {
      ArrayList<DiffChunk> chunks = new ArrayList<DiffChunk>();

      if (lines.size() == 0)
         return chunks;

      int line = lines.get(0).getOldLine();

      // The index of the earliest line that hasn't been put into a chunk yet
      int head = 0;

      for (int i = 1; i < lines.size(); i++)
      {
         if ((lines.get(i).getOldLine() - line) > 1)
         {
            // There's a gap between this line and the previous line. Turn
            // the previous contiguous run into a DiffChunk.

            List<Line> sublist = lines.subList(head, i);
            chunks.add(contiguousLinesToChunk(sublist));

            // This line is now the start of a new contiguous run.
            head = i;
         }
         line = lines.get(i).getOldLine();
      }

      // Add final contiguous run
      List<Line> sublist = lines.subList(head, lines.size());
      chunks.add(contiguousLinesToChunk(sublist));

      return chunks;
   }

   private DiffChunk contiguousLinesToChunk(List<Line> sublist)
   {
      Line first = sublist.get(0);
      Line last = sublist.get(sublist.size() - 1);
      return new DiffChunk(first.getOldLine(),
                           1 + last.getOldLine() - first.getOldLine(),
                           first.getNewLine(),
                           1 + last.getNewLine() - first.getNewLine(),
                           "",
                           new ArrayList<Line>(sublist));
   }

   /**
    * Here is where the heavy lifting of merging is done
    */
   private ArrayList<Line> generateOutputLines()
   {
      // Clean up contextLines_ so it only contains lines that are part of
      // the original document.
      for (int i = 0; i < contextLines_.size(); i++)
         if (contextLines_.get(i).getType() == Type.Insertion)
            contextLines_.remove(i--);

      // Clean up diffLines_ so it only contains lines that represent actual
      // changes. If we don't do this then the merge logic gets very confusing!
      for (int i = 0; i < diffLines_.size(); i++)
         if (diffLines_.get(i).getType() == Type.Same)
            diffLines_.remove(i--);

      // Check to see if maybe there's nothing to do
      if (diffLines_.size() == 0)
         return new ArrayList<Line>();

      // It's quite possible that the same DiffChunk was added multiple times.
      // (Less likely--maybe impossible--is for overlapping DiffChunks to be
      // added, but that would be dealt with by this as long as those DiffChunks
      // contain consistent data.)
      Collections.sort(contextLines_);
      DuplicateHelper.dedupeSortedList(contextLines_);

      // Clean up all the diff lines as well.
      Collections.sort(diffLines_);
      DuplicateHelper.dedupeSortedList(diffLines_);

      final ArrayList<Line> output = new ArrayList<Line>();

      final Iterator<Line> ctxit = contextLines_.iterator();
      final Iterator<Line> dffit = diffLines_.iterator();
      Line ctx = ctxit.hasNext() ? ctxit.next() : null;
      Line dff = dffit.hasNext() ? dffit.next() : null;

      /**
       * Now we have two ordered iterators, one for the context (original
       * document) and one for the diffs we want to apply to it. We want to
       * merge them together into the output ArrayList in the proper order,
       * being careful to throw out any context lines that are made obsolete
       * by the diff lines.
       */

      // Tracks the amount that the "new" line numbers are offset from the "old"
      // line numbers. new = old + skew
      int skew = 0;

      // Do this while loop while both iterators still have elements
      while (ctx != null && dff != null)
      {
         // Now we have a context line (ctx) and a diff line (dff) in hand.

         int cmp = ctx.getOldLine() - dff.getOldLine();
         if (cmp == 0 && ctx.equals(dff))
         {
            /**
             * ctx and dff are identical. And since we dropped Insertions from
             * contextLines_ and Sames from diffLines_, we know they're
             * Deletions. The dff takes precedence; we need to discard ctx so
             * the line actually gets deleted.
             */
            ctx = ctxit.hasNext() ? ctxit.next() : null;
            continue;
         }

         // In the case where cmp == 0, the oldLine properties were equal but
         // the newLine properties were not. This means the diff is an
         // insertion. We let the ctx line go first so the insertion happens
         // in the right place.
         if (cmp <= 0)
         {
            processContextLine(output, ctx, skew);
            ctx = ctxit.hasNext() ? ctxit.next() : null;
         }
         else
         {
            skew = processDiffLine(output, dff, skew);
            dff = dffit.hasNext() ? dffit.next() : null;
         }
      }

      // Finish off the context iterator if necessary
      while (ctx != null)
      {
         processContextLine(output, ctx, skew);
         ctx = ctxit.hasNext() ? ctxit.next() : null;
      }

      // Finish off the diff iterator if necessary
      while (dff != null)
      {
         skew = processDiffLine(output, dff, skew);
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
         default:
            assert false : "Unexpected context line type";
            throw new IllegalStateException();
      }
   }

   private int processDiffLine(ArrayList<Line> output, Line dff, int skew)
   {
      switch (dff.getType())
      {
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
            break;
         default:
            assert false : "Unexpected diff line type";
            throw new IllegalStateException();
      }
      return skew;
   }

   private final ArrayList<Line> contextLines_ = new ArrayList<Line>();
   private final ArrayList<Line> diffLines_ = new ArrayList<Line>();
   private final String fileA_;
   private final String fileB_;
   private static final String EOL = "\n";
}
