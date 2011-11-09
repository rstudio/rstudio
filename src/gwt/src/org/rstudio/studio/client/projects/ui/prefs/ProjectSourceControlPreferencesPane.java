package org.rstudio.studio.client.projects.ui.prefs;


import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.vcs.SshKeyChooser;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptionsDefault;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectSourceControlPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectSourceControlPreferencesPane(Session session,
                                              VCSServerOperations server)
   {
      // populate the vcs selections list
      String[] vcsSelections = new String[] { NONE };
      SessionInfo sessionInfo = session.getSessionInfo();
      if (sessionInfo.isVcsAvailable())
      {
         String[] availableVcs = sessionInfo.getAvailableVCS();
         vcsSelections = new String[availableVcs.length + 1];
         vcsSelections[0] = NONE;
         for (int i=0; i<availableVcs.length; i++)
            vcsSelections[i+1] = availableVcs[i];
      }
      
      vcsSelect_ = new SelectWidget("Version control system:", vcsSelections); 
      extraSpaced(vcsSelect_);
      add(vcsSelect_);
      vcsSelect_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            manageSshKeyVisibility();
         }
         
      });
      
      
      sshKeyChooser_ = new SshKeyChooser(
            server, 
            session.getSessionInfo().getDefaultSSHKeyDir(),
            "250px");
      nudgeRight(sshKeyChooser_);
      add(sshKeyChooser_);
      if (SshKeyChooser.isSupportedForCurrentPlatform())
         extraSpaced(sshKeyChooser_);
      else
         sshKeyChooser_.setVisible(false);
      
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
      defaultVcsOptions_ = options.getVcsOptionsDefault();
      RProjectVcsOptions vcsOptions = options.getVcsOptions();
      
      // set override or default
      if (vcsOptions.getActiveVcsOverride().length() > 0)
         setVcsSelection(vcsOptions.getActiveVcsOverride());
      else
         setVcsSelection(defaultVcsOptions_.getActiveVcs());
      
      // set override or default
      if (vcsOptions.getSshKeyPathOverride().length() > 0)
         sshKeyChooser_.setSshKey(vcsOptions.getSshKeyPathOverride());
      else
         sshKeyChooser_.setSshKey(defaultVcsOptions_.getSshKeyPath());
      
      
      manageSshKeyVisibility();
   }

   @Override
   public void onApply(RProjectOptions options)
   {
      RProjectVcsOptions vcsOptions = options.getVcsOptions();
      
      String vcsSelection = getVcsSelection();
      if (!vcsSelection.equals(defaultVcsOptions_.getActiveVcs()))
         vcsOptions.setActiveVcsOverride(vcsSelection);
      else
         vcsOptions.setActiveVcsOverride("");
      
      if (!sshKeyChooser_.getSshKey().equals(defaultVcsOptions_.getSshKeyPath()))
      {
         vcsOptions.setSshKeyPathOverride(sshKeyChooser_.getSshKey());
      }
      else
      {
         vcsOptions.setSshKeyPathOverride("");
      }
   }

   
   private String getVcsSelection()
   {
      String value = vcsSelect_.getValue();
      if (value.equals(NONE))
         return "none";
      else
         return value;
   }
   
   private void setVcsSelection(String vcs)
   {
      if (vcs.equals("none"))
         vcsSelect_.setValue(NONE);
      else if (!vcsSelect_.setValue(vcs))
      {
         vcsSelect_.setValue(NONE);
      }      
   }
   
   private void manageSshKeyVisibility()
   {
      sshKeyChooser_.setVisible(!getVcsSelection().equals("none"));
   }
   
   private SelectWidget vcsSelect_;
   private SshKeyChooser sshKeyChooser_;
   
   private RProjectVcsOptionsDefault defaultVcsOptions_;
   
   private static final String NONE = "(None)";
}
