/*
 * ProjectSourceControlPreferencesPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.prefs;


import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.SshKeyChooser;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptionsDefault;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class ProjectSourceControlPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectSourceControlPreferencesPane(final Session session,
                                              GlobalDisplay globalDisplay,
                                              EventBus eventBus,
                                              VCSServerOperations server)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
      
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
            
            if (vcsSelect_.getValue().equals("git"))
            {
               confirmGitRepo(new Command() {
                  @Override
                  public void execute()
                  {
                     promptToRestart(); 
                  }       
               });
            }
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
      if (defaultVcsOptions_.getSshKeyPath().length() > 0)
         sshKeyChooser_.setDefaultSskKey(defaultVcsOptions_.getSshKeyPath());
      
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
      setVcsOptions(vcsOptions);
   }
   
   private void setVcsOptions(RProjectVcsOptions vcsOptions)
   {
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
      
      manageSshKeyVisibility();
   }
   
   private void confirmGitRepo(final Command onConfirmed)
   {
      final ProgressIndicator indicator = getProgressIndicator();
      indicator.onProgress("Checking for git repository...");
      
      server_.vcsHasRepo(new ServerRequestCallback<Boolean>() {

         @Override
         public void onResponseReceived(Boolean result)
         {
            indicator.onCompleted();
            
            if (result)
            {
               onConfirmed.execute();
            }
            else
            {
               globalDisplay_.showYesNoMessage(
                  MessageDialog.QUESTION, 
                  "Confirm New Git Repository", 
                  "Do you want to initialize a new git repository " +
                  "for this project?", 
                  false,
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        server_.vcsInitRepo(
                          new VoidServerRequestCallback(indicator) {
                             @Override
                             public void onSuccess()
                             {
                                onConfirmed.execute();
                             }
                             @Override 
                             public void onFailure()
                             {
                                setVcsSelection("none");
                             }
                          });
                        
                     }
                  }, 
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        setVcsSelection("none");
                        indicator.onCompleted();
                     }
                     
                  },
                  true);
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            setVcsSelection("none");
            indicator.onError(error.getUserMessage());  
         }
         
      });
      
   }
   
   private void promptToRestart()
   {
      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION,
         "Confirm Restart RStudio", 
         "You need to restart RStudio in order to start working with " +
         "the specified version control system. Do you want to do this now?",
         new Operation()
         {
            @Override
            public void execute()
            {
               forceClosed(new Command() {

                  @Override
                  public void execute()
                  {
                     SwitchToProjectEvent event = new SwitchToProjectEvent(
                           session_.getSessionInfo().getActiveProjectFile());
                     eventBus_.fireEvent(event);
                     
                  }
                  
               });
            }  
         },
         true);
   }
   
   private void manageSshKeyVisibility()
   {
      sshKeyChooser_.setVisible(!getVcsSelection().equals("none"));
   }
   
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final VCSServerOperations server_;
   
   private SelectWidget vcsSelect_;
   private SshKeyChooser sshKeyChooser_;
  
   private RProjectVcsOptionsDefault defaultVcsOptions_;
   
   private static final String NONE = "(None)";
}
