/*
 * NotebookDocQueue.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class NotebookDocQueue extends JavaScriptObject
{
   protected NotebookDocQueue()
   {
   }
   
   public static final native NotebookDocQueue create(
         String docId, String jobDesc, int pixelWidth, int charWidth) /*-{
      return {
         doc_id:      docId,
         job_desc:    jobDesc,
         pixel_width: pixelWidth,
         char_width:  charWidth,
         units:       []
      }
   }-*/;
   
   public final native NotebookQueueUnit addUnit(NotebookQueueUnit unit) /*-{
      this.units.push(unit);
   }-*/;
   
   public final native NotebookQueueUnit removeUnit(NotebookQueueUnit unit) /*-{
      var idx = this.units.indexOf(unit);
      if (idx >= 0)
      {
         this.units.splice(idx, 1);
      }
   }-*/;
   
   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;

   public final native String getJobDesc() /*-{
      return this.job_desc;
   }-*/;

   public final native int getPixelWidth() /*-{
      return this.pixel_width;
   }-*/;

   public final native int getCharWidth() /*-{
      return this.char_width;
   }-*/;

   public final native JsArray<NotebookQueueUnit> getUnits() /*-{
      return this.units;
   }-*/;
   
   public final static int QUEUE_OP_ADD    = 0;
   public final static int QUEUE_OP_UPDATE = 1;
   public final static int QUEUE_OP_DELETE = 2;
   
   public final static int CHUNK_EXEC_STARTED  = 0;
   public final static int CHUNK_EXEC_FINISHED = 1;
}
