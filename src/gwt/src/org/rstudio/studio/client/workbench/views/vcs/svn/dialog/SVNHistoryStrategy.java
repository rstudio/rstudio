/*
 * SVNHistoryStrategy.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.dialog;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.view.client.HasData;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryStrategy;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

public class SVNHistoryStrategy implements HistoryStrategy
{
   @Inject
   public SVNHistoryStrategy(SVNServerOperations server,
                             SVNHistoryAsyncDataProvider dataProvider,
                             SVNState vcsState)
   {
      server_ = server;
      dataProvider_ = dataProvider;
      vcsState_ = vcsState;
   }

   @Override
   public void setRev(String rev)
   {
      dataProvider_.setRev(rev);
   }

   @Override
   public String idColumnName()
   {
      return "Revision";
   }

   @Override
   public boolean isBranchingSupported()
   {
      return false;
   }

   @Override
   public boolean isShowFileSupported()
   {
      return false;
   }

   @Override
   public void setSearchText(HasValue<String> searchText)
   {
      dataProvider_.setSearchText(searchText);
   }

   @Override
   public void setFileFilter(HasValue<FileSystemItem> fileFilter)
   {
      dataProvider_.setFileFilter(fileFilter);
   }

   @Override
   public void showFile(String revision,
                        String filename,
                        ServerRequestCallback<String> requestCallback)
   {
      int rev = parseRevision(revision);
      server_.svnShowFile(rev, filename, requestCallback);
   }

   @Override
   public HandlerRegistration addVcsRefreshHandler(VcsRefreshHandler handler)
   {
      return vcsState_.addVcsRefreshHandler(handler, false);
   }

   @Override
   public void showCommit(String commitId,
                          boolean noSizeWarning,
                          ServerRequestCallback<String> requestCallback)
   {
      int rev = parseRevision(commitId);
      server_.svnShow(rev, noSizeWarning, requestCallback);
   }

   @Override
   public void addDataDisplay(HasData<CommitInfo> display)
   {
      if (initialized_)
         dataProvider_.addDataDisplay(display);
   }

   @Override
   public void onRangeChanged(HasData<CommitInfo> display)
   {
      if (initialized_)
         dataProvider_.onRangeChanged(display);
   }

   @Override
   public void refreshCount()
   {
      if (initialized_)
         dataProvider_.refreshCount();
   }

   @Override
   public void initializeHistory(final HasData<CommitInfo> dataDisplay)
   {
      // Run a very short svnHistory call before allowing initialization to
      // proceed. We do this to force authentication to happen in a predictable
      // way, whereas without this mechanism, three different auth prompts
      // happen at the same time.

      server_.svnHistory(
            -1, null, 0, 1, "",
            new ServerRequestCallback<RpcObjectList<CommitInfo>>()
            {
               @Override
               public void onResponseReceived(RpcObjectList<CommitInfo> infos)
               {
                  initialized_ = true;

                  addDataDisplay(dataDisplay);
               }

               @Override
               public void onError(ServerError error)
               {
                  /* TODO: This may need to do a retry or something, depending
                     on how it goes with JJ's refactor of SVN auth */
                  Debug.logError(error);
               }
            });
   }

   private int parseRevision(String revision)
   {
      int rev;
      try
      {
         rev = Integer.parseInt(revision);
      }
      catch (NumberFormatException nfe)
      {
         rev = -1;
      }
      return rev;
   }

   private boolean initialized_;
   private final SVNServerOperations server_;
   private final SVNHistoryAsyncDataProvider dataProvider_;
   private final SVNState vcsState_;
}
