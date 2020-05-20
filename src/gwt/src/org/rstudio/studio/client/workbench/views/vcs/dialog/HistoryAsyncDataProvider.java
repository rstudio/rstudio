/*
 * HistoryAsyncDataProvider.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.user.cellview.client.AbstractHasData;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public abstract class HistoryAsyncDataProvider extends AsyncDataProvider<CommitInfo>
{
   @Inject
   public HistoryAsyncDataProvider()
   {
      rev_ = "";
      searchText_ = new Value<String>("");
      fileFilter_ = new Value<FileSystemItem>(null);
   }
   
   public void setHistoryStrategy(HistoryStrategy strategy)
   {
      strategy_ = strategy;
   }

   @Override
   public void addDataDisplay(HasData<CommitInfo> display)
   {
      super.addDataDisplay(display);
   }

   public void setSearchText(HasValue<String> searchText)
   {
      searchText_ = searchText;
   }
   
   public void setFileFilter(HasValue<FileSystemItem> fileFilter)
   {
      fileFilter_ = fileFilter;
   }
   
   public void setRev(String rev)
   {
      rev_ = rev;
   }
   
   

   public void refreshCount()
   {
      getHistoryCount(
            rev_, 
            fileFilter_.getValue(), 
            searchText_.getValue(), 
            new ServerRequestCallback<CommitCount>()
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
   public void onRangeChanged(final HasData<CommitInfo> display)
   {      
      final Range rng = display.getVisibleRange();
      final int start = rng.getStart();
      final int length = rng.getLength();

      if (length == 0)
         return;

      getHistory(
            rev_, fileFilter_.getValue(),
            start, length, searchText_.getValue(),
            new SimpleRequestCallback<RpcObjectList<CommitInfo>>("Error Fetching History")
            {
               @Override
               public void onResponseReceived(RpcObjectList<CommitInfo> response)
               {
                  super.onResponseReceived(response);
                  if (response.length() < length)
                     updateRowCount(start + response.length(), true);
                  updateRowData(start, response.toArrayList());
               }

               @Override
               public void onError(ServerError error)
               {
                  if (display instanceof AbstractHasData)
                  {
                     display.setVisibleRangeAndClearData(new Range(start, 0), true);
                  }
                  if (strategy_.getShowHistoryErrors())
                     super.onError(error);
                  else
                     Debug.logError(error);
               }
            });
   }

   protected abstract void getHistoryCount(
         String revision,
         FileSystemItem fileFilter,
         String searchText,
         ServerRequestCallback<CommitCount> requestCallback);

   protected abstract void getHistory(
         String revision,
         FileSystemItem fileFilter,
         int skip,
         int maxEntries,
         String searchText,
         ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   private String rev_;
   private HasValue<String> searchText_;
   private HasValue<FileSystemItem> fileFilter_;
   private HistoryStrategy strategy_;
}
