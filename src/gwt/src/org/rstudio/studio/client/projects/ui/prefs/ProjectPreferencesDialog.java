/*
 * ProjectPreferencesDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.apache.commons.lang3.StringEscapeUtils;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.packrat.PackratUtil;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectPackratOptions;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectBuildToolsPreferencesPane;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.util.tools.shared.StringUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   public static final int GENERAL = 0;
   public static final int EDITING = 1;
   public static final int SWEAVE = 2;
   public static final int BUILD = 3;
   public static final int VCS = 4;
   public static final int PACKRAT = 5;
   
   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UIPrefs> pUIPrefs,
                                   Provider<EventBus> pEventBus,
                                   Provider<PackratUtil> pPackratUtil,
                                   Provider<Session> session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build,
                                   ProjectPackratPreferencesPane packrat)
   {      
      super("Project Options",
            RES.styles().panelContainer(),
            false,
            new ProjectPreferencesPane[] {general, editing, compilePdf, build, 
                                          source, packrat});
           
      session_ = session;
      server_ = server;
      pUIPrefs_ = pUIPrefs;
      pEventBus_ = pEventBus;
      pPackratUtil_ = pPackratUtil;
   }
   
   @Override
   public void initialize(RProjectOptions options)
   {
      super.initialize(options);
      
      initialPackratOptions_ = options.getPackratOptions();
      
      if (!session_.get().getSessionInfo().getAllowVcs())
         hidePane(VCS);
      
      if (!options.getPackratContext().isAvailable())
         hidePane(PACKRAT);
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
                                final boolean reload)
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
                UIPrefs uiPrefs = pUIPrefs_.get();
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
                emitPackratConsoleActions(options.getPackratOptions());
                
                if (onCompleted != null)
                   onCompleted.execute();
                if (reload)
                   reload();
             }

             @Override
             public void onError(ServerError error)
             {
                indicator.onError(error.getUserMessage());
             }         
          });
      
   }
   
   private void emitPackratConsoleActions(RProjectPackratOptions options)
   {
      StringBuilder b = new StringBuilder();
      
      if (options.getAutoSnapshot() != initialPackratOptions_.getAutoSnapshot())
         b.append(packratOption("auto.snapshot", options.getAutoSnapshot()));

      if (options.getVcsIgnoreLib() != initialPackratOptions_.getVcsIgnoreLib())
         b.append(packratOption("vcs.ignore.lib", options.getVcsIgnoreLib()));
      
      if (options.getVcsIgnoreSrc() != initialPackratOptions_.getVcsIgnoreSrc())
         b.append(packratOption("vcs.ignore.src", options.getVcsIgnoreSrc()));
      
      if (options.getUseCache() != initialPackratOptions_.getUseCache())
         b.append(packratOption("use.cache", options.getUseCache()));
      
      if (options.getExternalPackages() != initialPackratOptions_.getExternalPackages())
         b.append(packratOption("external.packages", options.getExternalPackages()));
      
      if (options.getLocalRepos() != initialPackratOptions_.getLocalRepos())
         b.append(packratOption("local.repos", options.getLocalRepos()));
      
      // remove trailing newline
      if (b.length() > 0)
         b.deleteCharAt(b.length()-1);
      
      pEventBus_.get().fireEvent(new SendToConsoleEvent(b.toString(), 
                                                        true, 
                                                        true));
      
   }
   
   private String packratOption(String name, String value)
   {
      String args = name + " = " + "\"" + value.replaceAll("\"", "\\\\\"")+ "\"";
      String projectArg = pPackratUtil_.get().packratProjectArg();
      if (projectArg.length() > 0)
         args = args + ", " + projectArg;
      return "packrat::set_opts(" + args + ")\n";
   }
   
   private String packratOption(String name, boolean value)
   {
      String args = name + " = " + (value ? "TRUE" : "FALSE");
      String projectArg = pPackratUtil_.get().packratProjectArg();
      if (projectArg.length() > 0)
         args = args + ", " + projectArg;
      return "packrat::set_opts(" + args + ")\n";
      
   }
   
   private final Provider<Session> session_;
   private final ProjectsServerOperations server_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final Provider<EventBus> pEventBus_;
   private final Provider<PackratUtil> pPackratUtil_;
   
   private RProjectPackratOptions initialPackratOptions_ = null;
   
   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;


  
}
