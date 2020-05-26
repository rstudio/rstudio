/*
 * SVNState.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.model;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.common.model.VcsState;

@Singleton
public class SVNState extends VcsState
{
   @Inject
   public SVNState(SVNServerOperations server,
                   EventBus eventBus,
                   GlobalDisplay globalDisplay,
                   Session session)
   {
      super(eventBus, globalDisplay, session);
      server_ = server;
   }

   @Override
   protected StatusAndPathInfo getStatusFromFile(FileSystemItem file)
   {
      return file.getSVNStatus();
   }

   @Override
   protected boolean needsFullRefresh(FileSystemItem file)
   {
      return false;
   }

   @Override
   public void refresh(final boolean showError)
   {
      server_.svnStatus(new ServerRequestCallback<JsArray<StatusAndPathInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<StatusAndPathInfo> response)
         {
            status_ = StatusAndPath.fromInfos(response);
            handlers_.fireEvent(new VcsRefreshEvent(Reason.VcsOperation));
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            if (showError)
               globalDisplay_.showErrorMessage("Error",
                                               error.getUserMessage());
         }
      });
   }

   @Override
   protected boolean isInitialized()
   {
      return status_ != null;
   }

   private final SVNServerOperations server_;
}
