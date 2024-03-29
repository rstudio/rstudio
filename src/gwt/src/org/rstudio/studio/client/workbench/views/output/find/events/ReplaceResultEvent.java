/*
 * ReplaceResultEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;
import java.util.ArrayList;

public class ReplaceResultEvent extends GwtEvent<ReplaceResultEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onReplaceResult(ReplaceResultEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final String getHandle() /*-{
         return this.handle;
      }-*/;

      public native final RpcObjectList<FindResult> getResults() /*-{
         return this.results;
      }-*/;
   }

   public ReplaceResultEvent(String handle, ArrayList<FindResult> results)
   {
      handle_ = handle;
      results_ = results;
   }

   public String getHandle()
   {
      return handle_;
   }

   public ArrayList<FindResult> getResults()
   {
      return results_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onReplaceResult(this);
   }

   private final String handle_;
   private final ArrayList<FindResult> results_;

   public static final Type<Handler> TYPE = new Type<>();
}
