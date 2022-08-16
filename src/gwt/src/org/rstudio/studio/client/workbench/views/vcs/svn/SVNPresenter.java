/*
 * SVNPresenter.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.BaseVcsPresenter;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.ProcessCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.model.GitHubViewRequest;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNPresenter extends BaseVcsPresenter
{
   public interface Binder extends CommandBinder<Commands, SVNPresenter>
   {
   }

   public interface Display extends WorkbenchView, IsWidget, SVNPresenterDisplay
   {
      void setItems(ArrayList<StatusAndPath> items);

      void showContextMenu(int clientX, int clientY);
   }

   @Inject
   public SVNPresenter(Display view,
                       GlobalDisplay globalDisplay,
                       Binder binder,
                       Commands commands,
                       SVNServerOperations server,
                       SVNState svnState,
                       SatelliteManager satelliteManager,
                       VCSFileOpener vcsFileOpener)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      server_ = server;
      svnState_ = svnState;
      satelliteManager_ = satelliteManager;

      commandHandler_ = new SVNCommandHandler(view,
                                              globalDisplay,
                                              commands,
                                              server,
                                              svnState,
                                              vcsFileOpener);

      binder.bind(commands, this);

      svnState_.addVcsRefreshHandler(new VcsRefreshEvent.Handler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(svnState_.getStatus());
         }
      });

      view_.getChangelistTable().addContextMenuHandler(new ContextMenuHandler(){
         @Override
         public void onContextMenu(ContextMenuEvent event)
         {
            NativeEvent nativeEvent = event.getNativeEvent();
            view_.showContextMenu(nativeEvent.getClientX(),
                                  nativeEvent.getClientY());
         }
      });
   }

   private void showChanges(ArrayList<StatusAndPath> items)
   {
      showReviewPane(false, null, items);
   }

   private void showReviewPane(boolean showHistory,
                               FileSystemItem historyFileFilter,
                               ArrayList<StatusAndPath> items)
   {
      // setup params
      VCSApplicationParams params = VCSApplicationParams.create(
                                          showHistory,
                                          historyFileFilter,
                                          items);

      // open the window
      satelliteManager_.openSatellite("review_changes",
                                      params,
                                      new Size(1000,1200));
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Handler
   void onVcsDiff()
   {
      showChanges(view_.getSelectedItems());
   }

   // the following commands are BaseVcsPresenter overrides rather than
   // direct handlers (they are handled within the base class and then
   // dispatched to via these overrides)

   @Override
   public void onVcsCommit()
   {
      commandHandler_.onVcsCommit();
   }

   @Override
   public void onVcsShowHistory()
   {
      showHistory(null);
   }

   @Override
   public void onVcsPull()
   {
      commandHandler_.onVcsPull();
   }

   @Override
   public void onVcsPullRebase()
   {
      commandHandler_.onVcsPullRebase();
   }

   @Override
   public void onVcsCleanup()
   {
      server_.svnCleanup(new ProcessCallback(
                                         constants_.svnCleanup(),
                                         constants_.cleaningUpWorkingDirectoryEllipses(),
                                         750)); // pad progress for feedback
   }

   @Override
   public void onVcsIgnore()
   {
      commandHandler_.onVcsIgnore();
   }

   @Override
   public void showHistory(FileSystemItem fileFilter)
   {
      showReviewPane(true, fileFilter, new ArrayList<>());
   }

   @Override
   public void showDiff(FileSystemItem file)
   {
      // build an ArrayList<StatusAndPath> so we can call the core helper
      ArrayList<StatusAndPath> diffList = new ArrayList<>();
      for (StatusAndPath item :  svnState_.getStatus())
      {
         if (item.getRawPath() == file.getPath())
         {
            diffList.add(item);
            break;
         }
      }

      if (diffList.size() > 0)
      {
         showChanges(diffList);
      }
      else
      {
         globalDisplay_.showMessage(MessageDialog.INFO,
                                    constants_.noChangesToFile(),
                                    constants_.noChangesToFileTODiff(file.getName()));
      }

   }

   @Override
   public void revertFile(FileSystemItem file)
   {
      commandHandler_.revertFile(file);
   }


   @Override
   public void onVcsPush()
   {
      // git specific,  not supported by svn
   }

   @Override
   public void viewOnGitHub(GitHubViewRequest viewRequest)
   {
      // git specific, not supported by svn
   }


   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final SVNServerOperations server_;
   private final SVNCommandHandler commandHandler_;
   private final SVNState svnState_;
   private final SatelliteManager satelliteManager_;
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);

}
