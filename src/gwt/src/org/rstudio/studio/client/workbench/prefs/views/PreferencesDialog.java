/*
 * PreferencesDialog.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

public class PreferencesDialog extends PreferencesDialogBase<UserPrefs>
{
   /**
    * Implementation note, when adding a new PreferencesPane, also add a
    * command for directly showing that pane from the Command Palette. See
    * WorkbenchScreen.onShowCodeOptions() for an example.
    */
   @Inject
   public PreferencesDialog(WorkbenchServerOperations server,
                            Session session,
                            PreferencesDialogResources res,
                            GeneralPreferencesPane general,
                            EditingPreferencesPane source,
                            ConsolePreferencesPane console,
                            RMarkdownPreferencesPane rmarkdown,
                            CompilePdfPreferencesPane compilePdf,
                            AppearancePreferencesPane appearance,
                            PaneLayoutPreferencesPane paneLayout,
                            PackagesPreferencesPane packages,
                            SourceControlPreferencesPane sourceControl,
                            SpellingPreferencesPane spelling,
                            PublishingPreferencesPane publishing,
                            TerminalPreferencesPane terminal,
                            AccessibilityPreferencesPane accessibility,
                            PythonPreferencesPane python,
                            ApplicationQuit quit,
                            GlobalDisplay globalDisplay,
                            UserPrefs userPrefs,
                            UserState userState)
   {
      super("Options",
            res.styles().panelContainer(),
            res.styles().panelContainerNoChooser(),
            true,
            panes(
                  general,
                  source,
                  console,
                  appearance,
                  paneLayout,
                  packages,
                  rmarkdown,
                  compilePdf,
                  spelling,
                  sourceControl,
                  publishing,
                  terminal,
                  accessibility,
                  python));
      
      session_ = session;
      server_ = server;
      state_ = userState;
      quit_ = quit;
      globalDisplay_ = globalDisplay;

      if (!session.getSessionInfo().getAllowVcs())
         hidePane(SourceControlPreferencesPane.class);

      if (!session.getSessionInfo().getAllowPublish())
         hidePane(PublishingPreferencesPane.class);

      else if (!session.getSessionInfo().getAllowExternalPublish() &&
               !userState.enableRsconnectPublishUi().getValue())
      {
         hidePane(PublishingPreferencesPane.class);
      }

      if (!session.getSessionInfo().getAllowShell())
      {
         hidePane(TerminalPreferencesPane.class);
      }

      ElementIds.assignElementId(this, ElementIds.DIALOG_GLOBAL_PREFS);
   }

   @Override
   protected UserPrefs createEmptyPrefs()
   {
      return RStudioGinjector.INSTANCE.getUserPrefs();
   }

   @Override
   protected void doSaveChanges(final UserPrefs rPrefs,
                                final Operation onCompleted,
                                final ProgressIndicator progressIndicator,
                                final RestartRequirement restartRequirement)
   {
      // save changes
      server_.setUserPrefs(
         rPrefs.getUserLayer(),
         new SimpleRequestCallback<Void>() {

            @Override
            public void onResponseReceived(Void response)
            {
               // write accompanying state changes
               state_.writeState();

               progressIndicator.onCompleted();
               
               if (onCompleted != null)
                  onCompleted.execute();
               
               handleRestart(
                     globalDisplay_,
                     quit_,
                     session_,
                     restartRequirement);
            }

            @Override
            public void onError(ServerError error)
            {
               progressIndicator.onError(error.getUserMessage());
            }
         });

      // broadcast UI pref changes to satellites
      RStudioGinjector.INSTANCE.getSatelliteManager().dispatchCrossWindowEvent(
                     new UserPrefsChangedEvent(session_.getSessionInfo().getUserPrefLayer()));
   }

   public static void ensureStylesInjected()
   {
      GWT.<PreferencesDialogResources>create(PreferencesDialogResources.class).styles().ensureInjected();
   }
   
   @SafeVarargs
   private static final List<PreferencesDialogPaneBase<UserPrefs>> panes(
      PreferencesDialogPaneBase<UserPrefs>... paneList)
   {
      List<PreferencesDialogPaneBase<UserPrefs>> allPanes = new ArrayList<>();
      for (PreferencesDialogPaneBase<UserPrefs> pane : paneList)
         allPanes.add(pane);
      return allPanes;
   }
   

   private final WorkbenchServerOperations server_;
   private final Session session_;
   private final UserState state_;
   private final ApplicationQuit quit_;
   private final GlobalDisplay globalDisplay_;
}
