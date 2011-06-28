package org.rstudio.studio.client.workbench.views.vcs.diff;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import java.util.ArrayList;

public class UnifiedParser
{

   public static class DiffChunk
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

      public final int oldRowStart;
      public final int oldRowCount;
      public final int newRowStart;
      public final int newRowCount;
      public final String lineText;
      public final ArrayList<Line> diffLines;
   }

   public UnifiedParser(String data)
   {
      data_ = data;
   }

   public Pair<String, String> nextFilePair()
   {
      String line;
      while (null != (line = nextLine()) && !line.startsWith("--- "))
      {
      }

      if (line == null)
         return null;

      String fileA = line.substring(4);
      line = nextLine();
      if (line == null || !line.startsWith("+++ "))
         throw new DiffFormatException("Incomplete file header");
      String fileB = line.substring(4);
      return new Pair<String, String>(fileA, fileB);
   }

   public DiffChunk nextChunk()
   {
      String line;
      while (null != (line = nextLine()) && !(line.startsWith("@@ ") || line.startsWith("--- ")))
      {
      }

      if (line == null)
         return null;

      Match match = range_.match(line, 0);
      if (match == null)
         throw new DiffFormatException("Malformed chunk header");

      final int oldRowStart = Integer.parseInt(match.getGroup(1));
      final int oldCount = Integer.parseInt(match.getGroup(2));
      final int newRowStart = Integer.parseInt(match.getGroup(3));
      final int newCount = Integer.parseInt(match.getGroup(4));
      final String text = match.getGroup(6);

      int oldRow = oldRowStart;
      int oldRowsLeft = oldCount;
      int newRow = newRowStart;
      int newRowsLeft = newCount;

      ArrayList<Line> lines = new ArrayList<Line>();
      while (oldRowsLeft > 0 || newRowsLeft > 0)
      {
         String diffLine = nextLine();
         if (diffLine == null)
            throw new DiffFormatException("Diff ended prematurely");
         if (diffLine.length() == 0)
            throw new DiffFormatException("Unexpected blank line");
         switch (diffLine.charAt(0))
         {
            case ' ':
               oldRowsLeft--;
               newRowsLeft--;
               lines.add(Line.createSame(oldRow++,
                                         newRow++,
                                         diffLine.substring(1)));
               break;
            case '-':
               oldRowsLeft--;
               lines.add(Line.createDel(oldRow++, diffLine.substring(1)));
               break;
            case '+':
               newRowsLeft--;
               lines.add(Line.createIns(newRow++, diffLine.substring(1)));
               break;
            default:
               throw new DiffFormatException("Unexpected leading character");
         }

         if (oldRowsLeft < 0 || newRowsLeft < 0)
            throw new DiffFormatException("Diff ended prematurely");
      }

      return new DiffChunk(oldRowStart, oldCount, newRowStart, newCount,
                           text, lines);
   }

   private boolean isEOL()
   {
      return pos_ >= data_.length();
   }

   private String nextLine()
   {
      if (isEOL())
         return null;

      Match match = newline_.match(data_, pos_);
      if (match == null)
      {
         pos_ = data_.length();
         return data_.substring(pos_);
      }
      else
      {
         String value = data_.substring(pos_, match.getIndex());
         pos_ = match.getIndex() + match.getValue().length();
         return value;
      }
   }

   private final String data_;
   private int pos_;
   private final Pattern newline_ = Pattern.create("\\r?\\n");
   private final Pattern range_ = Pattern.create("^@@\\s*-([\\d]+),([\\d]+)\\s+\\+([\\d]+),([\\d]+)\\s*@@( (.*))?$", "m");
}
