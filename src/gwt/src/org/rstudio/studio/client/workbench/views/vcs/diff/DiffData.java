package org.rstudio.studio.client.workbench.views.vcs.diff;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.vcs.diff.UnifiedParser.DiffChunk;

import java.util.ArrayList;

public class DiffData extends JavaScriptObject
{
   protected DiffData() {}

   public native final String getOriginal() /*-{
      return this.original;
   }-*/;

   public native final String getDiff() /*-{
      return this.diff;
   }-*/;

   public final ArrayList<Line> toLines()
   {
      UnifiedParser parser = new UnifiedParser(getDiff());
      parser.nextFilePair();

      ArrayList<String> oldLines = new ArrayList<String>();
      oldLines.add(""); // fake a 1-based array
      for (String line : StringUtil.getLineIterator(getOriginal()))
         oldLines.add(line);

      ArrayList<Line> mergedLines = new ArrayList<Line>();

      int line = 1; // The line of the original document that we're currently on
      int drift = 0; // The difference between the new line num and old line num
      for (DiffChunk chunk = parser.nextChunk();
           chunk != null;
           chunk = parser.nextChunk())
      {
         while (line < chunk.oldRowStart)
         {
            mergedLines.add(Line.createSame(line, line + drift,
                                            oldLines.get(line)));
            line++;
         }

         if (chunk.newRowStart - chunk.oldRowStart != drift)
            throw new DiffFormatException("Row count integrity failure");

         mergedLines.addAll(chunk.diffLines);

         drift = (chunk.newRowStart + chunk.newRowCount) -
                 (chunk.oldRowStart + chunk.oldRowCount);
      }

      while (line < oldLines.size())
      {
         mergedLines.add(Line.createSame(line, line + drift,
                                         oldLines.get(line)));
      }

      return mergedLines;
   }
}
