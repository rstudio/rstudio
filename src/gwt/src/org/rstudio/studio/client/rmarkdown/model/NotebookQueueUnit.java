/*
 * NotebookQueueUnit.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class NotebookQueueUnit extends JavaScriptObject
{
   protected NotebookQueueUnit()
   {
   }
   
   public final static native NotebookQueueUnit create(
         String docId, String chunkId, String code) /*-{
      return {
         doc_id:    docId,
         chunk_id:  chunkId,
         code:      code,
         pending:   [],
         completed: [],
         executing: null
      };
   }-*/;
   
   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;

   public final native String getChunkId() /*-{
      return this.chunk_id;
   }-*/;

   public final native String getCode() /*-{
      return this.code;
   }-*/;
   
   public final native JsArray<NotebookExecRange> getPending() /*-{
      return this.pending;
   }-*/;

   public final native JsArray<NotebookExecRange> getCompleted() /*-{
      return this.completed;
   }-*/;
   
   public final native NotebookExecRange getExecuting() /*-{
      return this.executing;
   }-*/;
   
   public final native void addPendingRange(NotebookExecRange range) /*-{
      this.pending.push(range);
   }-*/;
   
   public final native void setExecutingRange(NotebookExecRange range) /*-{
      this.executing = range;
   }-*/;

   public final List<Integer> getPendingLines() 
   {
      return linesFromRanges(getPending());
   }
   
   public final List<Integer> getCompletedLines() 
   {
      return linesFromRanges(getCompleted());
   }
   
   public final List<Integer> getExecutingLines() 
   {
      return linesFromRange(getExecuting());
   }
   
   public final List<Integer> linesFromRange(NotebookExecRange range)
   {
      JsArray<NotebookExecRange> ranges = JsArray.createArray().cast();
      ranges.push(range);
      return linesFromRanges(ranges);
   }
   
   private final List<Integer> linesFromRanges(
         JsArray<NotebookExecRange> ranges)
   {
      List<Integer> lines = new ArrayList<Integer>();
      final String code = getCode();
      int line = 0;
      for (int i = 0; i < code.length(); i++)
      {
         // increment line counter if we're on a new line
         if (code.charAt(i) == '\n')
            line++;

         // skip if this line was already accounted for
         if (!lines.isEmpty() &&
             lines.get(lines.size() - 1) == line)
            continue;
         
         // check to see if this line is included in any of the given ranges
         for (int j = 0; j < ranges.length(); j++)
         {
            final NotebookExecRange range = ranges.get(j);
            if (i >= range.getStart() && i < range.getStop())
            {
               lines.add(line);
               continue;
            }
         }
      }
      
      return lines;
   }
}
