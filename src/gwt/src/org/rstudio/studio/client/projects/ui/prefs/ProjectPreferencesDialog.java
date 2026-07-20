/*
 * ProjectPreferencesDialog.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRenvOptions;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectBuildToolsPreferencesPane;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectAssistantPreferencesPane;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   // Numerical order of this enum must match visual order of the
   // project preferences dialog panes.
   public static final int GENERAL    = 0;
   public static final int EDITING    = 1;
   public static final int APPEARANCE = 2;
   public static final int R_MARKDOWN = 3;
   public static final int PYTHON     = 4;
   public static final int SWEAVE     = 5;
   public static final int SPELLING   = 6;
   public static final int BUILD      = 7;
   public static final int VCS        = 8;
   public static final int RENV       = 9;
   public static final int SHARING    = 10;

   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UserPrefs> pUIPrefs,
                                   Provider<UserState> pUserState,
                                   Provider<EventBus> pEventBus,
                                   Provider<Session> session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectAppearancePreferencesPane appearance,
                                   ProjectRMarkdownPreferencesPane rMarkdown,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSpellingPreferencesPane spelling,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build,
                                   ProjectRenvPreferencesPane renv,
                                   ProjectPythonPreferencesPane python,
                                   ProjectSharingPreferencesPane sharing,
                                   ProjectAssistantPreferencesPane assistant,
                                   Provider<ApplicationQuit> pQuit,
                                   Provider<GlobalDisplay> pGlobalDisplay)
   {
      super(constants_.projectOptionsCaption(),
            RES.styles().panelContainer(),
            RES.styles().panelContainerNoChooser(),
            false,
            panes(
                  general,
                  editing,
                  appearance,
                  rMarkdown,
                  python,
                  compilePdf,
                  spelling,
                  build,
                  source,
                  renv,
                  sharing,
                  assistant));

      pSession_ = session;
      server_ = server;
      pUIPrefs_ = pUIPrefs;
      pEventBus_ = pEventBus;
      pQuit_ = pQuit;
      pGlobalDisplay_ = pGlobalDisplay;
      appearance_ = appearance;
      pUserState_ = pUserState;
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

      // Hide the AI (assistant) pane when no AI feature is available -- either
      // compiled out (RSTUDIO_ENABLE_AI_FEATURES=OFF, or both GitHub Copilot and
      // Posit Assistant disabled) or disabled by the administrator.
      if (!pSession_.get().getSessionInfo().getCopilotEnabled() &&
          !pSession_.get().getSessionInfo().getPositAssistantEnabled())
      {
         hidePane(ProjectAssistantPreferencesPane.class);
      }
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
          new ServerRequestCallback<VoidResponse>() {
             @Override
             public void onResponseReceived(VoidResponse response)
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
                uiPrefs.projectName().setProjectValue(config.getProjectName());

                // native pipe prefs (remove if set to defaults)
                switch (config.getUseNativePipeOperator())
                {
                   case RProjectConfig.DEFAULT_VALUE:
                      uiPrefs.insertNativePipeOperator().removeProjectValue(true);
                      break;
                   case RProjectConfig.YES_VALUE:
                      uiPrefs.insertNativePipeOperator().setProjectValue(true);
                      break;
                   case RProjectConfig.NO_VALUE:
                      uiPrefs.insertNativePipeOperator().setProjectValue(false);
                      break;
                }

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

                // editor theme: drop the project override when (Default), else set it
                String projectTheme = config.getEditorTheme();
                if (StringUtil.isNullOrEmpty(projectTheme))
                   uiPrefs.editorTheme().removeProjectValue(true);
                else
                   uiPrefs.editorTheme().setProjectValue(projectTheme);

                // apply the effective theme live (handles (Default) and uninstalled
                // project themes); resolveAppliedTheme is null only if the theme list
                // is unavailable (not yet loaded or empty), in which case syncThemePrefs
                // applies on next project open
                AceTheme appliedTheme = appearance_.resolveAppliedTheme(uiPrefs);
                if (appliedTheme != null)
                   pUserState_.get().theme().setGlobalValue(appliedTheme);
                else
                   Debug.logWarning(
                      "Project editor theme saved but not applied live (theme list " +
                      "not loaded yet); it will take effect on the next project open.");

                // convert packrat option changes to console actions
                emitRenvConsoleActions(options.getRenvOptions());

                if (onCompleted != null)
                   onCompleted.execute();

                RStudioGinjector.INSTANCE.getEventBus().fireEvent(new ProjectOptionsChangedEvent(options));
                
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
   private final ProjectAppearancePreferencesPane appearance_;
   private final Provider<UserState> pUserState_;

   private RProjectRenvOptions renvOptions_;

   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;

   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);


}
