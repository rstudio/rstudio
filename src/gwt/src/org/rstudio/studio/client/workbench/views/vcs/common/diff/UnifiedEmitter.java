/*
 * UnifiedEmitter.java
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

import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line.Type;

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
      contextLines_.addAll(chunk.getLines());
   }

   public void addDiffs(ArrayList<Line> lines)
   {
      diffLines_.addAll(lines);
   }

   public String createPatch(boolean includeFileHeader)
   {
      prepareList(contextLines_, Type.Insertion);
      prepareList(diffLines_, Type.Same);
      final ArrayList<DiffChunk> chunks = toDiffChunks(
            new OutputLinesGenerator(contextLines_, diffLines_).getOutput());

      if (chunks.size() == 0)
         return "";

      StringBuilder p = new StringBuilder();

      // Write file header
      if (includeFileHeader)
      {
         p.append("--- ").append(fileA_).append(EOL);
         p.append("+++ ").append(fileB_).append(EOL);
      }

      for (DiffChunk chunk : chunks)
      {
         p.append(createChunkString(chunk));

         p.append(EOL);

         for (Line line : chunk.getLines())
         {
            switch (line.getType())
            {
               case Same:       p.append(' '); break;
               case Insertion:  p.append('+'); break;
               case Deletion:   p.append('-'); break;
               case Comment:    p.append('\\'); break;
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

      Range[] ranges = chunk.getRanges();
      for (int i = 0; i < ranges.length; i++)
         sb.append('@');

      for (int i = 0; i < ranges.length - 1; i++)
      {
         sb.append(" -").append(ranges[i].startRow);
         sb.append(',').append(ranges[i].rowCount);
      }
      sb.append(" +").append(ranges[ranges.length-1].startRow);
      sb.append(",").append(ranges[ranges.length-1].rowCount);

      sb.append(' ');
      for (int i = 0; i < ranges.length; i++)
         sb.append('@');

      sb.append(chunk.getLineText());

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

      int[] firstLines = first.getLines();
      int[] lastLines = last.getLines();

      // for purposes of chunk generation we need to not have any rows indicated
      // as zero
      for (int i = 0; i < firstLines.length; i++)
         firstLines[i] = Math.max(1, firstLines[i]);
      for (int i = 0; i < lastLines.length; i++)
         lastLines[i] = Math.max(1, lastLines[i]);

      Range[] ranges = new Range[firstLines.length];
      for (int i = 0; i < firstLines.length; i++)
         ranges[i] = new Range(firstLines[i], 1 + lastLines[i] - firstLines[i]);

      return new DiffChunk(ranges,
                           "",
                           new ArrayList<Line>(sublist),
                           -1);
   }

   private static void prepareList(ArrayList<Line> lines, Type typeToRemove)
   {
      // Remove any entries that match the given type
      for (int i = 0; i < lines.size(); i++)
         if (lines.get(i).getType() == typeToRemove)
            lines.remove(i--);

      // Sort and deduplicate
      Collections.sort(lines);
      DuplicateHelper.dedupeSortedList(lines);
   }

   /**
    * Here is where the heavy lifting of merging is done. The only reason this
    * is factored into a class is to make up for the lack of real closures in
    * Java.
    */
   private static class OutputLinesGenerator
   {
      private final ArrayList<Line> output = new ArrayList<Line>();

      private final Iterator<Line> ctxit; // Iterator for all context lines
      private final Iterator<Line> dffit; // Iterator for all diff lines
      private Line ctx; // Points to the current context line
      private Line dff; // Points to the current diff line

      // Tracks the amount that the "new" line numbers are offset from the "old"
      // line numbers. new = old + skew
      private int skew = 0;

      private OutputLinesGenerator(ArrayList<Line> contextLines,
                                   ArrayList<Line> diffLines)
      {
         ctxit = contextLines.iterator();
         dffit = diffLines.iterator();

         // Set ctx and dff to first lines (or null if empty)
         ctxPop(false);
         dffPop(false);

         /**
          * Now we have two ordered iterators, one for the context (original
          * document) and one for the diffs we want to apply to it. We want to
          * merge them together into the output ArrayList in the proper order,
          * being careful to throw out any context lines that are made obsolete
          * by the diff lines.
          */

         // Do this while loop while both iterators still have elements
         while (ctx != null && dff != null)
         {
            // Now we have a context line (ctx) and a diff line (dff) in hand.

/*
            Debug.devlogf("DiffIndex: {0}/{1}, {2}-{3}, {4}-{5}",
                          ctx.getDiffIndex(),
                          dff.getDiffIndex(),
                          ctx.getOldLine(),
                          ctx.getNewLine(),
                          dff.getOldLine(),
                          dff.getNewLine());
*/
            int cmp = ctx.getDiffIndex() - dff.getDiffIndex();
            if (cmp < 0)
               ctxPop(true);
            else if (cmp > 0)
               dffPop(true);
            else
            {
               /**
                * ctx and dff are identical. And since we dropped all Insertions
                * from contextLines_ and all Sames from diffLines_, we know they
                * must be either Deletions or Comments.
                */
               if (ctx.getType() == Type.Deletion)
               {
                  dffPop(true);
                  ctxPop(false);
               }
               else if (ctx.getType() == Type.Comment)
               {
                  dffPop(false);
                  ctxPop(true);
               }
               else
               {
                  throw new IllegalStateException(
                        "Unexpected line type: " + ctx.getType().name());
               }
            }
         }

         // Finish off the context iterator if necessary
         while (ctx != null)
            ctxPop(true);

         // Finish off the diff iterator if necessary
         while (dff != null)
            dffPop(true);
      }

      /**
       * (Optionally) adds the value of ctx to the output, and then (always)
       * sets ctx to the next context line
       */
      private void ctxPop(boolean addToOutput)
      {
         if (addToOutput)
            writeContextLine(output, ctx, skew);
         ctx = ctxit.hasNext() ? ctxit.next() : null;
      }

      /**
       * (Optionally) adds the value of dff to the output, and then (always)
       * sets dff to the next diff line
       */
      private void dffPop(boolean addToOutput)
      {
         if (addToOutput)
            skew = writeDiffLine(output, dff, skew);
         dff = dffit.hasNext() ? dffit.next() : null;
      }

      private void writeContextLine(ArrayList<Line> output, Line ctx, int skew)
      {
         switch (ctx.getType())
         {
            case Same:
            case Comment:
               output.add(new Line(ctx.getType(), ctx.getOldLine(),
                                   ctx.getOldLine() + skew,
                                   ctx.getText(),
                                   ctx.getDiffIndex()));
               break;
            case Deletion:
               // This is a line that, in the source diff, was deleted from orig.
               // But since we're processing it as context, we ignore the delete,
               // so we turn it back into "Same".
               output.add(new Line(Type.Same, ctx.getOldLine(),
                                   ctx.getOldLine() + skew,
                                   ctx.getText(),
                                   ctx.getDiffIndex()));
               break;
            default:
               assert false : "Unexpected context line type";
               throw new IllegalStateException();
         }
      }

      private int writeDiffLine(ArrayList<Line> output, Line dff, int skew)
      {
         switch (dff.getType())
         {
            case Deletion:
               output.add(new Line(Type.Deletion, dff.getOldLine(),
                                   dff.getOldLine() + skew,
                                   dff.getText(),
                                   dff.getDiffIndex()));
               skew--;
               break;
            case Insertion:
               output.add(new Line(Type.Insertion, dff.getOldLine(),
                                   dff.getOldLine() + skew,
                                   dff.getText(),
                                   dff.getDiffIndex()));
               skew++;
               break;
            default:
               assert false : "Unexpected diff line type";
               throw new IllegalStateException();
         }
         return skew;
      }

      /**
       * Get the result
       */
      public ArrayList<Line> getOutput()
      {
         return output;
      }
   }

   private final ArrayList<Line> contextLines_ = new ArrayList<Line>();
   private final ArrayList<Line> diffLines_ = new ArrayList<Line>();
   private final String fileA_;
   private final String fileB_;
   private static final String EOL = "\n";
}
