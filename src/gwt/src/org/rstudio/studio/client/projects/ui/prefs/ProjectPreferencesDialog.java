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

import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectBuildToolsPreferencesPane;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   public static final int GENERAL = 0;
   public static final int EDITING = 1;
   public static final int SWEAVE = 2;
   public static final int BUILD = 3;
   public static final int VCS = 4;
   
   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UIPrefs> pUIPrefs,
                                   Session session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build)
   {      
      super("Project Options",
            RES.styles().panelContainer(),
            false,
            new ProjectPreferencesPane[] {general, editing, compilePdf, build, source});
           
      server_ = server;
      pUIPrefs_ = pUIPrefs;  
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
   
 
   private final ProjectsServerOperations server_;
   private final Provider<UIPrefs> pUIPrefs_;
   
   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;


  
}
