/*
 * HistoryAsyncDataProvider.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class HistoryAsyncDataProvider extends AsyncDataProvider<CommitInfo>
{
   @Inject
   public HistoryAsyncDataProvider(GitServerOperations server)
   {
      server_ = server;
      rev_ = "";
      searchText_ = new Value<String>("");
   }

   @Override
   public void addDataDisplay(HasData<CommitInfo> display)
   {
      super.addDataDisplay(display);
      refreshCount();
   }

   public void setSearchText(HasValue<String> searchText)
   {
      searchText_ = searchText;
   }
   
   public void setRev(String rev)
   {
      rev_ = rev;
   }

   public void refreshCount()
   {
      server_.gitHistoryCount(rev_, searchText_.getValue(), new ServerRequestCallback<CommitCount>()
      {
         @Override
         public void onResponseReceived(CommitCount response)
         {
            updateRowCount(response.getCount(), true);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   @Override
   protected void onRangeChanged(final HasData<CommitInfo> display)
   {
      final Range rng = display.getVisibleRange();
      server_.gitHistory(
            rev_, rng.getStart(), rng.getLength(), searchText_.getValue(),
            new SimpleRequestCallback<RpcObjectList<CommitInfo>>("Error Fetching History")
            {
               @Override
               public void onResponseReceived(RpcObjectList<CommitInfo> response)
               {
                  super.onResponseReceived(response);
                  updateRowData(rng.getStart(), response.toArrayList());
               }
            });
   }

   private final GitServerOperations server_;
   private String rev_;
   private HasValue<String> searchText_;
}
