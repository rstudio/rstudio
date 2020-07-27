/*
 * NotebookRangeExecutedEvent.java
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

package org.rstudio.studio.client.rmarkdown.events;

import org.rstudio.studio.client.rmarkdown.model.NotebookExecRange;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NotebookRangeExecutedEvent
             extends GwtEvent<NotebookRangeExecutedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getDocId() /*-{
         return this.doc_id;
      }-*/;

      public final native String getChunkId() /*-{
         return this.chunk_id;
      }-*/;

      public final native String getCode() /*-{
         return this.code;
      }-*/;

      public final native NotebookExecRange getExecRange() /*-{
         return this.exec_range;
      }-*/;

      public final native int getExprMode() /*-{
         return this.expr_mode;
      }-*/;
   }


   public interface Handler extends EventHandler
   {
      void onNotebookRangeExecuted(NotebookRangeExecutedEvent event);
   }

   public NotebookRangeExecutedEvent(Data data)
   {
      data_ = data;
   }

   public String getDocId()
   {
      return data_.getDocId();
   }

   public String getChunkId()
   {
      return data_.getChunkId();
   }

   public String getCode()
   {
      return data_.getCode();
   }

   public NotebookExecRange getExecRange()
   {
      return data_.getExecRange();
   }

   public int getExprMode()
   {
      return data_.getExprMode();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNotebookRangeExecuted(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();

   public final static int EXPR_NEW          = 0;
   public final static int EXPR_CONTINUATION = 1;
}
