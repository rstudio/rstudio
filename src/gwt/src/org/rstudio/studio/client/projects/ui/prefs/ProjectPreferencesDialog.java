/*
 * ProjectPreferencesDialog.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRenvOptions;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectBuildToolsPreferencesPane;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   public static final int GENERAL    = 0;
   public static final int EDITING    = 1;
   public static final int R_MARKDOWN = 2;
   public static final int SWEAVE     = 3;
   public static final int SPELLING   = 4;
   public static final int BUILD      = 5;
   public static final int VCS        = 6;
   public static final int RENV       = 7;
   public static final int PYTHON     = 8;
   public static final int SHARING    = 9;

   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UserPrefs> pUIPrefs,
                                   Provider<UserState> pUserState,
                                   Provider<EventBus> pEventBus,
                                   Provider<Session> session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectRMarkdownPreferencesPane rMarkdown,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSpellingPreferencesPane spelling,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build,
                                   ProjectRenvPreferencesPane renv,
                                   ProjectPythonPreferencesPane python,
                                   ProjectSharingPreferencesPane sharing,
                                   Provider<ApplicationQuit> pQuit,
                                   Provider<GlobalDisplay> pGlobalDisplay)
   {
      super("Project Options",
            RES.styles().panelContainer(),
            RES.styles().panelContainerNoChooser(),
            false,
            panes(
                  general,
                  editing,
                  rMarkdown, 
                  compilePdf,
                  spelling,
                  build,
                  source,
                  renv,
                  python,
                  sharing));

      pSession_ = session;
      server_ = server;
      pUIPrefs_ = pUIPrefs;
      pEventBus_ = pEventBus;
      pQuit_ = pQuit;
      pGlobalDisplay_ = pGlobalDisplay;
   }

   @Override
   public void initialize(RProjectOptions options)
   {
      super.initialize(options);

      renvOptions_ = options.getRenvOptions();

      if (!pSession_.get().getSessionInfo().getAllowVcs())
         hidePane(VCS);

      if (!pSession_.get().getSessionInfo().projectSupportsSharing())
         hidePane(SHARING);
   }

   @Override
   protected RProjectOptions createEmptyPrefs()
   {
      return RProjectOptions.createEmpty();
   }


   @Override
   protected void doSaveChanges(final RProjectOptions options,
                                final Operation onCompleted,
                                final ProgressIndicator indicator,
                                final RestartRequirement restartRequirement)
   {

      server_.writeProjectOptions(
          options,
          new ServerRequestCallback<Void>() {
             @Override
             public void onResponseReceived(Void response)
             {
                indicator.onCompleted();

                // update project ui prefs
                RProjectConfig config = options.getConfig();
                UserPrefs uiPrefs = pUIPrefs_.get();
                
                uiPrefs.useSpacesForTab().setProjectValue(
                                           config.getUseSpacesForTab());
                uiPrefs.numSpacesForTab().setProjectValue(
                                           config.getNumSpacesForTab());
                uiPrefs.autoAppendNewline().setProjectValue(
                                           config.getAutoAppendNewline());
                uiPrefs.stripTrailingWhitespace().setProjectValue(
                                           config.getStripTrailingWhitespace());
                uiPrefs.defaultEncoding().setProjectValue(
                                           config.getEncoding());
                uiPrefs.defaultSweaveEngine().setProjectValue(
                                           config.getDefaultSweaveEngine());
                uiPrefs.defaultLatexProgram().setProjectValue(
                                           config.getDefaultLatexProgram());
                uiPrefs.rootDocument().setProjectValue(
                                           config.getRootDocument());
                uiPrefs.useRoxygen().setProjectValue(
                                           config.hasPackageRoxygenize());
                
                // markdown prefs (if they are set to defaults then remove the project prefs, otherwise forward them on)
                if (!config.getMarkdownWrap().equals(RProjectConfig.MARKDOWN_WRAP_DEFAULT))
                {
                   uiPrefs.visualMarkdownEditingWrap().setProjectValue(config.getMarkdownWrap());
                   uiPrefs.visualMarkdownEditingWrapAtColumn().setProjectValue(config.getMarkdownWrapAtColumn());
                }
                else
                {
                   uiPrefs.visualMarkdownEditingWrap().removeProjectValue(true);
                   uiPrefs.visualMarkdownEditingWrapAtColumn().removeProjectValue(true);
                }
                if (!config.getMarkdownReferences().equals(RProjectConfig.MARKDOWN_REFERENCES_DEFAULT))
                   uiPrefs.visualMarkdownEditingReferencesLocation().setProjectValue(config.getMarkdownReferences());
                else
                   uiPrefs.visualMarkdownEditingReferencesLocation().removeProjectValue(true);
                if (config.getMarkdownCanonical() != RProjectConfig.DEFAULT_VALUE)
                   uiPrefs.visualMarkdownEditingCanonical().setProjectValue(config.getMarkdownCanonical() == RProjectConfig.YES_VALUE);
                else
                   uiPrefs.visualMarkdownEditingCanonical().removeProjectValue(true);
                
                // zotero prefs (remove if set to defaults)
                if (config.getZoteroLibraries() != null)
                   uiPrefs.zoteroLibraries().setProjectValue(config.getZoteroLibraries());
                else
                   uiPrefs.zoteroLibraries().removeProjectValue(true);
                
                // propagate spelling prefs
                if (!config.getSpellingDictionary().isEmpty())
                   uiPrefs.spellingDictionaryLanguage().setProjectValue(config.getSpellingDictionary());
                else
                   uiPrefs.spellingDictionaryLanguage().removeProjectValue(true);

                // convert packrat option changes to console actions
                emitRenvConsoleActions(options.getRenvOptions());

                if (onCompleted != null)
                   onCompleted.execute();
                
                handleRestart(
                      pGlobalDisplay_.get(),
                      pQuit_.get(),
                      pSession_.get(),
                      restartRequirement);
             }

             @Override
             public void onError(ServerError error)
             {
                indicator.onError(error.getUserMessage());
             }
          });

   }

   private void emitRenvConsoleActions(RProjectRenvOptions options)
   {
      if (options.useRenv == renvOptions_.useRenv)
         return;

      String renvAction = options.useRenv
            ? "renv::activate()"
            : "renv::deactivate()";

      pEventBus_.get().fireEvent(new SendToConsoleEvent(renvAction, true, true));
   }
   
   @SafeVarargs
   private static final List<PreferencesDialogPaneBase<RProjectOptions>> panes(
      PreferencesDialogPaneBase<RProjectOptions>... paneList)
   {
      List<PreferencesDialogPaneBase<RProjectOptions>> allPanes = new ArrayList<>();
      for (PreferencesDialogPaneBase<RProjectOptions> pane : paneList)
         allPanes.add(pane);
      return allPanes;
   }

   private final Provider<Session> pSession_;
   private final ProjectsServerOperations server_;
   private final Provider<UserPrefs> pUIPrefs_;
   private final Provider<EventBus> pEventBus_;
   private final Provider<ApplicationQuit> pQuit_;
   private final Provider<GlobalDisplay> pGlobalDisplay_;

   private RProjectRenvOptions renvOptions_;

   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;



}
