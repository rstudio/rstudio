/*
 * ProjectPreferencesDialog.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
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
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   public static final int GENERAL = 0;
   public static final int EDITING = 1;
   public static final int SWEAVE = 2;
   public static final int BUILD = 3;
   public static final int VCS = 4;
   public static final int RENV = 5;
   public static final int SHARING = 6;
   
   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UserPrefs> pUIPrefs,
                                   Provider<EventBus> pEventBus,
                                   Provider<Session> session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build,
                                   ProjectRenvPreferencesPane renv,
                                   ProjectSharingPreferencesPane sharing,
                                   Provider<ApplicationQuit> pQuit,
                                   Provider<GlobalDisplay> pGlobalDisplay)
   {      
      super("Project Options",
            RES.styles().panelContainer(),
            false,
            new ProjectPreferencesPane[] {general, editing, compilePdf, build, 
                                          source, renv, sharing});
           
      session_ = session;
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
      
      if (!session_.get().getSessionInfo().getAllowVcs())
         hidePane(VCS);
      
      if (!session_.get().getSessionInfo().projectSupportsSharing())
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
                
                // convert packrat option changes to console actions
                emitRenvConsoleActions(options.getRenvOptions());
                
                if (onCompleted != null)
                   onCompleted.execute();
                if (restartRequirement.getDesktopRestartRequired())
                   restart(pGlobalDisplay_.get(), pQuit_.get(), session_.get());
                if (restartRequirement.getUiReloadRequired())
                   reload();
             }

             @Override
             public void onError(ServerError error)
             {
                indicator.onError(error.getUserMessage());
             }
          });
      
   }
   
   private void emitRenvConsoleActions(RProjectRenvOptions newOptions)
   {
      EventBus events = pEventBus_.get();
      
      // renv is off and staying off -- nothing to do
      if (!newOptions.useRenv && !renvOptions_.useRenv)
      {
      }
      
      // turning renv off -- just call deactivate
      else if (!newOptions.useRenv && renvOptions_.useRenv)
      {
         String action = "renv::deactivate()";
         events.fireEvent(new SendToConsoleEvent(action, true, true));
         return;
      }
      
      // turning renv on -- just call activate
      //
      // TODO: we need to apply project options here, but we need to call
      // renv::activate() first to activate the project ... and that will
      // force the R session to restart. is there a way for us to cleanly
      // orchestrate this chain of events?
      else if (newOptions.useRenv && !renvOptions_.useRenv)
      {
         String action = "renv::activate()";
         events.fireEvent(new SendToConsoleEvent(action, true, true));
         return;
      }
      else
      {
         // update project settings, etc
         List<String> commands = new ArrayList<>();

         if (renvOptions_.projectUseCache != newOptions.projectUseCache)
            commands.add(setting("use.cache", newOptions.projectUseCache));

         if (renvOptions_.projectVcsIgnoreLibrary != newOptions.projectVcsIgnoreLibrary)
            commands.add(setting("vcs.ignore.library", newOptions.projectVcsIgnoreLibrary));

         if (renvOptions_.userSandboxEnabled != newOptions.userSandboxEnabled)
            commands.add(config("sandbox.enabled", newOptions.userSandboxEnabled));

         if (renvOptions_.userShimsEnabled != newOptions.userShimsEnabled)
            commands.add(config("shims.enabled", newOptions.userShimsEnabled));

         if (renvOptions_.userUpdatesCheck != newOptions.userUpdatesCheck)
            commands.add(config("updates.check", newOptions.userUpdatesCheck));

         if (!commands.isEmpty())
         {
            String command = StringUtil.join(commands, "\n");
            events.fireEvent(new SendToConsoleEvent(command, true, true));
         }
      }
      
   }
   
   private String setting(String key, boolean value)
   {
      String toggle = (value ? "TRUE" : "FALSE");
      return "renv::settings$" + key + "(" + toggle + ")";
   }
   
   private String config(String key, boolean value)
   {
      String toggle = (value ? "TRUE" : "FALSE");
      return "options(renv.config." + key + " = " + toggle + ")";
   }
   
   private final Provider<Session> session_;
   private final ProjectsServerOperations server_;
   private final Provider<UserPrefs> pUIPrefs_;
   private final Provider<EventBus> pEventBus_;
   private final Provider<ApplicationQuit> pQuit_;
   private final Provider<GlobalDisplay> pGlobalDisplay_;
   
   private RProjectRenvOptions renvOptions_;
   
   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;


  
}
