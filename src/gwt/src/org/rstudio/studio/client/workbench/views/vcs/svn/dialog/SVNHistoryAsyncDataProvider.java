/*
 * SVNHistoryAsyncDataProvider.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.dialog;

import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryAsyncDataProvider;

public class SVNHistoryAsyncDataProvider extends HistoryAsyncDataProvider
{
   @Inject
   public SVNHistoryAsyncDataProvider(SVNServerOperations server)
   {
      server_ = server;
   }

   @Override
   protected void getHistoryCount(String revision,
                                  FileSystemItem fileFilter,
                                  String searchText,
                                  ServerRequestCallback<CommitCount> requestCallback)
   {
      server_.svnHistoryCount(StringUtil.parseInt(revision, -1),
                              fileFilter,
                              searchText,
                              requestCallback);
   }

   @Override
   protected void getHistory(String revision,
                             FileSystemItem fileFilter,
                             int skip,
                             int maxEntries,
                             String searchText,
                             ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback)
   {
      server_.svnHistory(StringUtil.parseInt(revision, -1),
                         fileFilter,
                         skip,
                         maxEntries,
                         searchText,
                         requestCallback);
   }

   private final SVNServerOperations server_;
}
