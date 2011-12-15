package org.rstudio.studio.client.workbench.views.vcs.git.dialog;

import com.google.inject.Inject;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryAsyncDataProvider;

public class GitHistoryAsyncDataProvider extends HistoryAsyncDataProvider
{
   @Inject
   public GitHistoryAsyncDataProvider(GitServerOperations server)
   {
      server_ = server;
   }

   @Override
   protected void getHistoryCount(String revision,
                                  FileSystemItem fileFilter,
                                  String searchText,
                                  ServerRequestCallback<CommitCount> requestCallback)
   {
      server_.gitHistoryCount(revision,
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
      server_.gitHistory(revision,
                         fileFilter,
                         skip,
                         maxEntries,
                         searchText,
                         requestCallback);
   }

   private final GitServerOperations server_;
}
