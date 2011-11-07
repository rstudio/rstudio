package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectConfig>
{
   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UIPrefs> pUIPrefs,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectSourceControlPreferencesPane source)
   {
      super("Project Options",
            RES.styles().panelContainer(),
            new ProjectPreferencesPane[] {general, editing, source});
      
      server_ = server;
      pUIPrefs_ = pUIPrefs;
      
  
   }
   
   public void activateSourceControl()
   {
      activatePane(2);
   }
   
   
   @Override
   protected RProjectConfig createEmptyPrefs()
   {
      return RProjectConfig.createEmpty();
   }
   
   
   @Override
   protected void doSaveChanges(final RProjectConfig prefs,
                                final Operation onCompleted,
                                final ProgressIndicator indicator)
   {
      
      server_.writeProjectConfig(
          prefs, 
          new ServerRequestCallback<Void>() {
             @Override
             public void onResponseReceived(Void response)
             {
                indicator.onCompleted();
                
                // update project ui prefs
                UIPrefs uiPrefs = pUIPrefs_.get();
                uiPrefs.useSpacesForTab().setProjectValue(
                                           prefs.getUseSpacesForTab());
                uiPrefs.numSpacesForTab().setProjectValue(
                                           prefs.getNumSpacesForTab());
                uiPrefs.defaultEncoding().setProjectValue(
                                           prefs.getEncoding());   
                
                if (onCompleted != null)
                   onCompleted.execute();
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
