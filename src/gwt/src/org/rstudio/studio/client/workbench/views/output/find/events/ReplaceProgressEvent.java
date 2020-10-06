/*
 * ReplaceProgressEvent.java
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
package org.rstudio.studio.client.workbench.views.output.find.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ReplaceProgressEvent extends GwtEvent<ReplaceProgressEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onReplaceProgress(ReplaceProgressEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final int getTotalReplaceCount() /*-{
         return this.totalReplaceCount;
      }-*/;

      public native final int getReplacedCount() /*-{
         return this.replacedCount;
      }-*/;
   }

   public ReplaceProgressEvent(int totalReplaceCount, int replacedCount)
   {
      totalReplaceCount_ = totalReplaceCount;
      replacedCount_ = replacedCount;
   }

   public int replacedCount()
   {
      return replacedCount_;
   }

   public int totalReplaceCount()
   {
      return totalReplaceCount_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onReplaceProgress(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
   private final int totalReplaceCount_;
   private final int replacedCount_;
}
