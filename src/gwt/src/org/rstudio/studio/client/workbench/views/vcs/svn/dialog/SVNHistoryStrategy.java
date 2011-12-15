package org.rstudio.studio.client.workbench.views.vcs.svn.dialog;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.view.client.HasData;
import com.google.inject.Inject;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
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
   public boolean isBranchingSupported()
   {
      return true;
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
      dataProvider_.addDataDisplay(display);
   }

   @Override
   public void onRangeChanged(HasData<CommitInfo> display)
   {
      dataProvider_.onRangeChanged(display);
   }

   @Override
   public void refreshCount()
   {
      dataProvider_.refreshCount();
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

   private final SVNServerOperations server_;
   private final SVNHistoryAsyncDataProvider dataProvider_;
   private final SVNState vcsState_;
}
