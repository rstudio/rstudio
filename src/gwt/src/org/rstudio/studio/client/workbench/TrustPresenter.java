/*
 * TrustPresenter.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.TrustRequestEvent;
import org.rstudio.studio.client.workbench.events.WorkbenchLoadedEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TrustPresenter implements WorkbenchLoadedEvent.Handler,
                                       DeferredInitCompletedEvent.Handler
{
   interface Binder extends CommandBinder<Commands, TrustPresenter> {}

   @Inject
   public TrustPresenter(EventBus eventBus,
                          Commands commands,
                          Session session,
                          Server server,
                          GlobalDisplay globalDisplay)
   {
      eventBus_ = eventBus;
      commands_ = commands;
      session_ = session;
      server_ = server;
      globalDisplay_ = globalDisplay;

      ((Binder) GWT.create(Binder.class)).bind(commands, this);
      eventBus.addHandler(WorkbenchLoadedEvent.TYPE, this);
      eventBus.addHandler(DeferredInitCompletedEvent.TYPE, this);

      // Initially hidden; shown when startup files are suppressed
      String lockSvg =
         "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' " +
         "viewBox='0 0 24 24' fill='none' stroke='currentColor' " +
         "stroke-linecap='round' stroke-linejoin='round'>" +
         "<path d='M7 9V7a5 5 0 0 1 10 0v2' stroke-width='2.5'/>" +
         "<rect x='4' y='9' width='16' height='14' rx='3' ry='3' " +
              "fill='currentColor' stroke='none'/>" +
         "</svg>";
      restrictedModeIcon_ = new HTML(lockSvg);
      restrictedModeIcon_.setTitle(appConstants_.restrictedModeTitle());
      restrictedModeIcon_.addStyleName(
            ThemeResources.INSTANCE.themeStyles().restrictedModeIcon());
      restrictedModeIcon_.setVisible(false);
      restrictedModeIcon_.addClickHandler(event ->
            commands_.showTrustRequestDialog().execute());
   }

   /**
    * Sets the initial visibility of the restricted mode icon based on
    * whether startup files were suppressed.
    */
   public void initializeForSession(SessionInfo sessionInfo)
   {
      restrictedModeIcon_.setVisible(sessionInfo.getStartupFilesSuppressed());
   }

   /**
    * Returns the restricted mode icon widget for inclusion in the toolbar.
    */
   public Widget getRestrictedModeIcon()
   {
      return restrictedModeIcon_;
   }

   @Override
   public void onWorkbenchLoaded(WorkbenchLoadedEvent event)
   {
      TrustRequestEvent.Data trustData =
         session_.getSessionInfo().getTrustRequest();
      if (trustData != null && trustData.getDirectory() != null)
      {
         if (trustStatusChanged(trustData))
            showTrustRequestDialog(trustData);
      }
   }

   @Override
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
   {
      JsObject data = event.getData().cast();
      if (data.hasKey("startup_files_suppressed"))
      {
         boolean suppressed = data.getBoolean("startup_files_suppressed");
         restrictedModeIcon_.setVisible(suppressed);
      }

      if (data.hasKey("trust_request"))
      {
         TrustRequestEvent.Data trustData = data.getObject("trust_request").cast();
         if (trustData != null && trustData.getDirectory() != null)
         {
            if (trustStatusChanged(trustData))
               showTrustRequestDialog(trustData);
         }
      }
   }

   @Handler
   void onShowTrustRequestDialog()
   {
      TrustRequestEvent.Data trustData =
         session_.getSessionInfo().getTrustRequest();
      if (trustData != null && trustData.getDirectory() != null)
      {
         showTrustRequestDialog(trustData);
      }
   }

   private boolean trustStatusChanged(TrustRequestEvent.Data data)
   {
      return lastTrustStatus_ == null || !lastTrustStatus_.equals(data.getStatus());
   }

   private void showTrustRequestDialog(TrustRequestEvent.Data data)
   {
      if (dialog_ != null)
      {
         dialog_.center();
         return;
      }

      lastTrustStatus_ = data.getStatus();
      String directory = data.getDirectory();

      dialog_ = new TrustRequestDialog(
         directory,
         data.getStatus(),
         data.getRiskyFiles(),
         () ->
         {
            server_.grantTrust(directory, new ServerRequestCallback<VoidResponse>()
            {
               @Override
               public void onResponseReceived(VoidResponse response)
               {
                  restrictedModeIcon_.setVisible(false);
                  eventBus_.fireEvent(new SuspendAndRestartEvent(
                     SuspendOptions.createSaveAll(false)));
               }

               @Override
               public void onError(ServerError error)
               {
                  globalDisplay_.showErrorMessage(
                     constants_.progressErrorCaption(),
                     error.getUserMessage());
               }
            });
         },
         () ->
         {
            // The toolbar lock icon already indicates restricted mode;
            // no additional feedback needed here.
            server_.revokeTrust(directory, new ServerRequestCallback<VoidResponse>()
            {
               @Override
               public void onResponseReceived(VoidResponse response)
               {
               }

               @Override
               public void onError(ServerError error)
               {
                  globalDisplay_.showErrorMessage(
                     constants_.progressErrorCaption(),
                     error.getUserMessage());
               }
            });
         });
      dialog_.addCloseHandler(event -> dialog_ = null);
      dialog_.showModal();
   }

   private final EventBus eventBus_;
   private final Commands commands_;
   private final Session session_;
   private final Server server_;
   private final GlobalDisplay globalDisplay_;
   private final HTML restrictedModeIcon_;
   private TrustRequestDialog dialog_;

   // tracks last trust status to suppress re-showing dialog on restart
   // with the same status
   private String lastTrustStatus_;

   private static final StudioClientApplicationConstants appConstants_ =
      GWT.create(StudioClientApplicationConstants.class);
   private static final ClientWorkbenchConstants constants_ =
      GWT.create(ClientWorkbenchConstants.class);
}
