/*
 * HistoryEntriesAddedEvent.java
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
package org.rstudio.studio.client.workbench.views.history.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;

public class HistoryEntriesAddedEvent extends GwtEvent<HistoryEntriesAddedHandler>
{
   public static final GwtEvent.Type<HistoryEntriesAddedHandler> TYPE =
      new GwtEvent.Type<HistoryEntriesAddedHandler>();
   
   public HistoryEntriesAddedEvent(RpcObjectList<HistoryEntry> entries)
   {
      entries_ = entries;
   }
   
   public RpcObjectList<HistoryEntry> getEntries()
   {
      return entries_;
   }
   
   @Override
   protected void dispatch(HistoryEntriesAddedHandler handler)
   {
      handler.onHistoryEntriesAdded(this);
   }

   @Override
   public GwtEvent.Type<HistoryEntriesAddedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final RpcObjectList<HistoryEntry> entries_;
}
