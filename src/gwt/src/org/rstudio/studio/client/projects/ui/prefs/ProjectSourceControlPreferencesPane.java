/*
 * ProjectSourceControlPreferencesPane.java
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


import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.common.vcs.VcsHelpLink;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsContext;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectSourceControlPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectSourceControlPreferencesPane(final Session session,
                                              GlobalDisplay globalDisplay,
                                              GitServerOperations server)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      vcsSelect_ = new SelectWidget("Version control system:", new String[]{}); 
      spaced(vcsSelect_);
      add(vcsSelect_);
      vcsSelect_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {  
            updateOriginLabel();
            
            if (vcsSelect_.getValue().equals(VCSConstants.GIT_ID))
            {
               confirmGitRepo(new Command() {
                  @Override
                  public void execute()
                  {
                     promptToRestart(); 
                  }       
               });
            }
            else
            {
               promptToRestart();
            }
         }
      });
      
      lblOrigin_ = new OriginLabel();
      lblOrigin_.addStyleName(RES.styles().vcsOriginLabel());
      lblOrigin_.addStyleName(ThemeStyles.INSTANCE.selectableText());
      extraSpaced(lblOrigin_);
      add(lblOrigin_);
      
      HelpLink vcsHelpLink = new VcsHelpLink();
      nudgeRight(vcsHelpLink);
      add(vcsHelpLink);
     
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconSourceControl2x());
   }

   @Override
   public String getName()
   {
      return "Git/SVN";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      // save the context
      vcsContext_ = options.getVcsContext();
      
      // populate the vcs selections list
      String[] vcsSelections = new String[] { NONE };
      JsArrayString applicableVcs = vcsContext_.getApplicableVcs();
      if (applicableVcs.length() > 0)
      {
         vcsSelections = new String[applicableVcs.length() + 1];
         vcsSelections[0] = NONE;
         for (int i=0; i<applicableVcs.length(); i++)
            vcsSelections[i+1] = applicableVcs.get(i);
      }
      vcsSelect_.setChoices(vcsSelections);
    
      // set override or default
      RProjectVcsOptions vcsOptions = options.getVcsOptions();
      if (vcsOptions.getActiveVcsOverride().length() > 0)
         setVcsSelection(vcsOptions.getActiveVcsOverride());
      else
         setVcsSelection(vcsContext_.getDetectedVcs());
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectVcsOptions vcsOptions = options.getVcsOptions();
      setVcsOptions(vcsOptions);
      return false;
   }
   
   private void setVcsOptions(RProjectVcsOptions vcsOptions)
   {
      String vcsSelection = getVcsSelection();
      if (!vcsSelection.equals(vcsContext_.getDetectedVcs()))
         vcsOptions.setActiveVcsOverride(vcsSelection);
      else
         vcsOptions.setActiveVcsOverride("");
   }

   
   private String getVcsSelection()
   {
      String value = vcsSelect_.getValue();
      if (value.equals(NONE))
         return VCSConstants.NO_ID;
      else
         return value;
   }
   
   private void setVcsSelection(String vcs)
   {
      // set value
      if (vcs.equals(VCSConstants.NO_ID))
         vcsSelect_.setValue(NONE);
      else if (!vcsSelect_.setValue(vcs))
      {
         vcsSelect_.setValue(NONE);
      }      
      
      updateOriginLabel();
      
      
   }
   
   private void updateOriginLabel()
   {
      String vcs = getVcsSelection();
      if (vcs.equals(VCSConstants.GIT_ID))
      {
         StringBuilder label = new StringBuilder();
         label.append("Origin: ");
         String originUrl = vcsContext_.getGitRemoteOriginUrl();
         if (originUrl.length() == 0)
            originUrl = NO_REMOTE_ORIGIN;
         lblOrigin_.setOrigin("Origin:", originUrl);
         lblOrigin_.setVisible(true);
         vcsSelect_.removeStyleName(RES.styles().vcsSelectExtraSpaced());
         
      }
      else if (vcs.equals(VCSConstants.SVN_ID))
      {
         lblOrigin_.setOrigin("Repo:", 
                              vcsContext_.getSvnRepositoryRoot());
         lblOrigin_.setVisible(true);
         vcsSelect_.removeStyleName(RES.styles().vcsSelectExtraSpaced());
      }
      else // vcs.equals(VCSConstants.NO_ID)
      {
         lblOrigin_.setOrigin("", "");
         lblOrigin_.setVisible(false);
         vcsSelect_.addStyleName(RES.styles().vcsSelectExtraSpaced());
      }
   }
   
   
   private void confirmGitRepo(final Command onConfirmed)
   {
      final ProgressIndicator indicator = getProgressIndicator();
      indicator.onProgress("Checking for git repository...");
      
      final String projDir = 
               session_.getSessionInfo().getActiveProjectDir().getPath();
      
      server_.gitHasRepo(projDir, new ServerRequestCallback<Boolean>() {

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
                        server_.gitInitRepo(
                          projDir,
                          new VoidServerRequestCallback(indicator) {
                             @Override
                             public void onSuccess()
                             {
                                onConfirmed.execute();
                             }
                             @Override 
                             public void onFailure()
                             {
                                setVcsSelection(VCSConstants.NO_ID);
                             }
                          });
                        
                     }
                  }, 
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        setVcsSelection(VCSConstants.NO_ID);
                        indicator.onCompleted();
                     }
                     
                  },
                  true);
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            setVcsSelection(VCSConstants.NO_ID);
            indicator.onError(error.getUserMessage());  
         }
         
      });
      
   }
      
   private class OriginLabel extends Composite
   {
      public OriginLabel()
      {
         HorizontalPanel panel = new HorizontalPanel();
         lblCaption_ = new Label();
         panel.add(lblCaption_);
         
         lblOrigin_ = new Label();
         lblOrigin_.addStyleName(RES.styles().vcsOriginUrl());
         panel.add(lblOrigin_);
         
         initWidget(panel);
         
         
      }
      
      public void setOrigin(String caption, String origin)
      {
         lblCaption_.setText(caption);
         lblOrigin_.setText(origin);
         
         if (origin.equals(NO_REMOTE_ORIGIN))
            lblOrigin_.addStyleName(RES.styles().vcsNoOriginUrl());
         else
            lblOrigin_.removeStyleName(RES.styles().vcsNoOriginUrl());
      }
        
      private Label lblCaption_;
      private Label lblOrigin_;
   }
   
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final GitServerOperations server_;
   
   private SelectWidget vcsSelect_;
   private OriginLabel lblOrigin_;
   private RProjectVcsContext vcsContext_;
   
   private static final String NONE = "(None)";
   
   private static final String NO_REMOTE_ORIGIN  ="None";
   
   private static final ProjectPreferencesDialogResources RES = 
                                    ProjectPreferencesDialogResources.INSTANCE;
}
