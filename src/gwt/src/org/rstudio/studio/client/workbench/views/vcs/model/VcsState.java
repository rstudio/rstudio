/*
 * VcsState.java
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
package org.rstudio.studio.client.workbench.views.vcs.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshHandler;

@Singleton
public class VcsState
{
   @Inject
   public VcsState(VCSServerOperations server,
                   EventBus eventBus,
                   GlobalDisplay globalDisplay)
   {
      server_ = server;
      eventBus_ = eventBus;
      globalDisplay_ = globalDisplay;
      eventBus_.addHandler(VcsRefreshEvent.TYPE, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            refresh(false);
         }
      });
      eventBus_.addHandler(FileChangeEvent.TYPE, new FileChangeHandler()
      {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            refresh(false);
         }
      });

      refresh(false);
   }

   public void bindRefreshHandler(Widget owner,
                                  final VcsRefreshHandler handler)
   {
      new WidgetHandlerRegistration(owner)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return addVcsRefreshHandler(handler);
         }
      };
   }

   public HandlerRegistration addVcsRefreshHandler(VcsRefreshHandler handler)
   {
      HandlerRegistration hreg = handlers_.addHandler(
            VcsRefreshEvent.TYPE, handler);

      if (branches_ != null)
         handler.onVcsRefresh(new VcsRefreshEvent());

      return hreg;
   }

   public JsArray<StatusAndPath> getStatus()
   {
      return status_;
   }

   public BranchesInfo getBranchInfo()
   {
      return branches_;
   }

   public void refresh()
   {
      refresh(true);
   }

   private void refresh(final boolean showError)
   {
      server_.vcsListBranches(new ServerRequestCallback<BranchesInfo>()
      {
         @Override
         public void onResponseReceived(BranchesInfo response)
         {
            branches_ = response;

            server_.vcsFullStatus(new ServerRequestCallback<JsArray<StatusAndPath>>()
            {
               @Override
               public void onResponseReceived(JsArray<StatusAndPath> response)
               {
                  status_ = response;

                  handlers_.fireEvent(new VcsRefreshEvent());
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
         public void onError(ServerError error)
         {
            Debug.logError(error);
            if (showError)
               globalDisplay_.showErrorMessage("Error",
                                               error.getUserMessage());
         }
      });
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private JsArray<StatusAndPath> status_;
   private BranchesInfo branches_;
   private final VCSServerOperations server_;
   private final EventBus eventBus_;
   private final GlobalDisplay globalDisplay_;
   private HandlerRegistration handlerRegistration_;
}
