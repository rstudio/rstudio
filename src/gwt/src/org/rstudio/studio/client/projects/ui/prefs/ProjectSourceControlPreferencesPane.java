package org.rstudio.studio.client.projects.ui.prefs;


import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptions;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectSourceControlPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectSourceControlPreferencesPane(Session session)
   {
      SessionInfo sessionInfo = session.getSessionInfo();
      String[] availableVcs = sessionInfo.getAvailableVCS();
      String[] vcsSelections = new String[availableVcs.length + 1];
      vcsSelections[0] = "(None)";
      for (int i=0; i<availableVcs.length; i++)
         vcsSelections[i+1] = availableVcs[i];
      
      
      vcsSelect_ = new SelectWidget("Project version control:", vcsSelections); 
      extraSpaced(vcsSelect_);
      add(vcsSelect_);
      
      VCSHelpLink vcsHelpLink = new VCSHelpLink();
      nudgeRight(vcsHelpLink);
      add(vcsHelpLink);
     
   }

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconSourceControl();
   }

   @Override
   public String getName()
   {
      return "Version Control";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void onApply(RProjectOptions options)
   {
      RProjectVcsOptions vcsOptions = options.getVcsOptions();
      vcsOptions.setActiveVcsOverride("");
      vcsOptions.setSshKeyPathOverride("");
      
   }

   
   private SelectWidget vcsSelect_;
}
