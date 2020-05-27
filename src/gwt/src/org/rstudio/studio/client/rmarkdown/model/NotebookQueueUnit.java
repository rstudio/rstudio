/*
 * NotebookQueueUnit.java
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
         String docId, String chunkId, int execMode, int execScope, 
         String code) /*-{
      return {
         doc_id:     docId,
         chunk_id:   chunkId,
         exec_mode:  execMode,
         exec_scope: execScope,
         code:       code,
         pending:    [],
         completed:  [],
         executing:  null
      };
   }-*/;
   
   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;

   public final native String getChunkId() /*-{
      return this.chunk_id;
   }-*/;
   
   public final native int getExecMode() /*-{
      return this.exec_mode;
   }-*/;
   
   public final native int getExecScope() /*-{
      return this.exec_scope;
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
   
   public final native void addCompletedRange(NotebookExecRange range) /*-{
      this.completed.push(range);
   }-*/;
   
   public final native void setExecutingRange(NotebookExecRange range) /*-{
      this.executing = range;
   }-*/;
   
   public final void extendExecutingRange(NotebookExecRange range) 
   {
      if (getExecuting() == null)
         setExecutingRange(range);
      else
         getExecuting().extendTo(range);
   }

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
   
   public final boolean hasPendingRange(NotebookExecRange range)
   {
      JsArray<NotebookExecRange> ranges = getPending();
      for (int i = 0; i < ranges.length(); i++)
      {
         if (ranges.get(i).getStart() >= range.getStart() &&
             ranges.get(i).getStop() <= range.getStop())
            return true;
      }
      return false;
   }
   
   private final List<Integer> linesFromRanges(
         JsArray<NotebookExecRange> ranges)
   {
      List<Integer> lines = new ArrayList<Integer>();
      final String code = getCode();
      int line = 0, last = -1;
      for (int i = 0; i < code.length(); i++)
      {
         // increment line counter if we're on a new line
         if (code.charAt(i) == '\n')
            line++;

         // skip if this line was already accounted for
         if (line == last)
            continue;
         
         // check to see if this line is included in any of the given ranges
         for (int j = 0; j < ranges.length(); j++)
         {
            final NotebookExecRange range = ranges.get(j);
            if (i >= range.getStart() && i < range.getStop())
            {
               // if this is a newline, include the previous line if not 
               // already included
               if (code.charAt(i) == '\n' && last != line - 1)
                  lines.add(line - 1);

               lines.add(line);
               last = line;
               continue;
            }
         }
      }
      
      return lines;
   }
   
   public final static int EXEC_SCOPE_CHUNK   = 0;
   public final static int EXEC_SCOPE_PARTIAL = 1;
   public final static int EXEC_SCOPE_INLINE  = 2;
   
   public final static int EXEC_MODE_SINGLE = 0;
   public final static int EXEC_MODE_BATCH  = 1;
}
