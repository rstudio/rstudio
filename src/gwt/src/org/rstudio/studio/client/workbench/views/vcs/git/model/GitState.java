/*
 * GitState.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.git.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.*;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.common.model.VcsState;

@Singleton
public class GitState extends VcsState
{
   @Inject
   public GitState(GitServerOperations server,
                   EventBus eventBus,
                   GlobalDisplay globalDisplay,
                   Session session)
   {
      super(eventBus, globalDisplay, session);
      server_ = server;
   }

   public BranchesInfo getBranchInfo()
   {
      return branches_;
   }

   public boolean hasRemote()
   {
      return getRemoteBranchInfo() != null;
   }

   public RemoteBranchInfo getRemoteBranchInfo()
   {
      return remoteBranchInfo_;
   }

   @Override
   protected boolean isInitialized()
   {
      return branches_ != null;
   }

   @Override
   protected StatusAndPathInfo getStatusFromFile(FileSystemItem file)
   {
      return file.getGitStatus();
   }

   @Override
   protected boolean needsFullRefresh(FileSystemItem file)
   {
      return file.getName().equalsIgnoreCase(".gitignore");
   }

   public void refresh(boolean showError)
   {
      refresh(showError, null);
   }

   public void refresh(final boolean showError, final Command onCompleted)
   {
      server_.gitAllStatus(false, new ServerRequestCallback<AllStatus>()
      {
         @Override
         public void onResponseReceived(AllStatus response)
         {
            status_ = StatusAndPath.fromInfos(response.getStatus());
            branches_ = response.getBranches();
            remoteBranchInfo_ = response.getRemoteBranchInfo();
            handlers_.fireEvent(new VcsRefreshEvent(Reason.VcsOperation));
            if (onCompleted != null)
               onCompleted.execute();
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            if (showError)
               globalDisplay_.showErrorMessage(constants_.errorCapitalized(),
                                               error.getUserMessage());
         }
      });
   }
   
   public void refreshMinimal()
   {
      server_.gitAllStatus(true, new ServerRequestCallback<AllStatus>()
      {
         @Override
         public void onResponseReceived(AllStatus response)
         {
            branches_ = response.getBranches();
            remoteBranchInfo_ = response.getRemoteBranchInfo();
            handlers_.fireEvent(new VcsRefreshEvent(Reason.VcsOperation));
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private BranchesInfo branches_;
   private RemoteBranchInfo remoteBranchInfo_;
   private final GitServerOperations server_;
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}