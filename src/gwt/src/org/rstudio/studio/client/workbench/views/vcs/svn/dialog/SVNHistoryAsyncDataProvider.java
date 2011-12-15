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
