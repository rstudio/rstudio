/*
 * OpenObjectExplorerEvent.java
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

package org.rstudio.studio.client.workbench.views.source.editors.explorer.events;

import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class OpenObjectExplorerEvent extends GwtEvent<OpenObjectExplorerEvent.Handler>
{
   public OpenObjectExplorerEvent(ObjectExplorerHandle handle)
   {
      handle_ = handle;
   }
   
   public ObjectExplorerHandle getHandle()
   {
      return handle_;
   }
   
   private final ObjectExplorerHandle handle_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onOpenObjectExplorerEvent(OpenObjectExplorerEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenObjectExplorerEvent(this);
   }
   
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
