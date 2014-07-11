/*
 * ProjectPackratPreferencesPane.java
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

import org.rstudio.core.client.widget.FixedTextArea;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.packrat.PackratUtil;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.packrat.model.PackratPrerequisites;
import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectPackratOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectPackratPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectPackratPreferencesPane(Session session,
                                        GlobalDisplay globalDisplay,
                                        PackratServerOperations server,
                                        PackratUtil packratUtil)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
      server_ = server;
      packratUtil_ = packratUtil;
   }

   @Override
   public ImageResource getIcon()
   {
      return ProjectPreferencesDialogResources.INSTANCE.iconPackrat();
   }

   @Override
   public String getName()
   {
      return "Packrat";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      Label label = new Label(
            "Packrat is a dependency management tool that makes your " +
            "R code more isolated, portable, and reproducible by " +
            "giving your project its own privately managed package " +
            "library."
        );
        spaced(label);
        add(label);
        
        PackratContext context = options.getPackratContext();
        RProjectPackratOptions packratOptions = options.getPackratOptions();
               
        usePackratButton_ = new ThemedButton(
           "Use Packrat with this Project",
           new ClickHandler() {

              @Override
              public void onClick(ClickEvent event)
              {
                 bootstrapPackrat();
              }
              
           });
        spaced(usePackratButton_);
        usePackratButton_.getElement().getStyle().setMarginTop(10, Unit.PX);
        add(usePackratButton_);
   
        chkUsePackrat_ = new CheckBox("Use packrat with this project");
        chkUsePackrat_.setValue(true);
        chkUsePackrat_.addValueChangeHandler(
                                new ValueChangeHandler<Boolean>() {

         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            confirmRemovePackrat();
         }
        });
       
        spaced(chkUsePackrat_);
        add(chkUsePackrat_);
        
        chkAutoSnapshot_ = new CheckBox("Automatically snapshot local changes");
        chkAutoSnapshot_.setValue(packratOptions.getAutoSnapshot());
        lessSpaced(chkAutoSnapshot_);
        add(chkAutoSnapshot_);
        
        String vcsName = session_.getSessionInfo().getVcsName();
        chkVcsIgnoreLib_ = new CheckBox(vcsName + " ignore packrat library"); 
        chkVcsIgnoreLib_.setValue(packratOptions.getVcsIgnoreLib());
        lessSpaced(chkVcsIgnoreLib_);
        add(chkVcsIgnoreLib_);
        
        chkVcsIgnoreSrc_ = new CheckBox(vcsName + " ignore packrat sources");
        chkVcsIgnoreSrc_.setValue(packratOptions.getVcsIgnoreSrc());
        lessSpaced(chkVcsIgnoreSrc_);
        add(chkVcsIgnoreSrc_);
        
        chkUseCache_ = new CheckBox("Use global cache for installed packages");
        chkUseCache_.setValue(packratOptions.getUseCache());
        spaced(chkUseCache_);
        add(chkUseCache_);
        
        panelExternalPackages_ = new VerticalPanel();
        panelExternalPackages_.add(
              new HTML("External packages (separate with comma or newline)"));
        taExternalPackages_ = new FixedTextArea(3, 45);
        taExternalPackages_.setText(packratOptions.getExternalPackages());
        panelExternalPackages_.add(taExternalPackages_);
        add(panelExternalPackages_);
        
        panelLocalRepos_ = new VerticalPanel();
        panelExternalPackages_.add(
              new HTML("Local repositories (separate with comma or newline)"));
        taLocalRepos_ = new FixedTextArea(3, 45);
        taLocalRepos_.setText(packratOptions.getLocalRepos());
        panelLocalRepos_.add(taLocalRepos_);
        add(panelLocalRepos_);
        
        manageUI(context.isPackified());

        HelpLink helpLink = new HelpLink("Learn more about Packrat", 
                                         "packrat", 
                                         false);
        helpLink.getElement().getStyle().setMarginTop(15, Unit.PX);
        nudgeRight(helpLink);
        add(helpLink);
   }
   
   private void manageUI(boolean packified)
   {
      boolean vcsActive = !session_.getSessionInfo().getVcsName().equals("");

      usePackratButton_.setVisible(!packified);
      chkUsePackrat_.setVisible(packified);
      chkAutoSnapshot_.setVisible(packified);
      chkUseCache_.setVisible(packified);
      panelExternalPackages_.setVisible(packified);
      panelLocalRepos_.setVisible(packified);
      chkVcsIgnoreLib_.setVisible(packified && vcsActive);
      chkVcsIgnoreSrc_.setVisible(packified && vcsActive);
   }
   
   private void confirmRemovePackrat()
   {
      globalDisplay_.showYesNoMessage(
          MessageDialog.QUESTION, 
          "Disable Packrat", 
          "Disabling packrat for this project will revert to the use of " +
          "standard user and system package libraries.\n\n" +
          "Disable packrat for this project now?",
          false,
          new Operation() {
            @Override
            public void execute()
            {
               packratUtil_.executePackratFunction("disable");
               
               chkUsePackrat_.setValue(true, false);
               manageUI(false);
            }   
          },
          new Operation() {
            @Override
            public void execute()
            {
               chkUsePackrat_.setValue(true, false);  
            }
          },
          true);
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectPackratOptions packratOptions = options.getPackratOptions();
      packratOptions.setAutoSnapshot(chkAutoSnapshot_.getValue());
      packratOptions.setVcsIgnoreLib(chkVcsIgnoreLib_.getValue());
      packratOptions.setVcsIgnoreSrc(chkVcsIgnoreSrc_.getValue());
      packratOptions.setUseCache(chkUseCache_.getValue());
      packratOptions.setExternalPackages(taExternalPackages_.getValue());
      packratOptions.setLocalRepos(taLocalRepos_.getValue());
      return false;
   }
  
   private void bootstrapPackrat()
   {
      final ProgressIndicator indicator = getProgressIndicator();
      
      indicator.onProgress("Verifying prequisites...");
      
      server_.getPackratPrerequisites(
        new ServerRequestCallback<PackratPrerequisites>() {
           @Override
           public void onResponseReceived(PackratPrerequisites prereqs)
           {
              indicator.onCompleted();
              
              if (prereqs.getBuildToolsAvailable())
              {
                 executeBootstrap(prereqs);
              }
              else
              {
                 // install build tools (with short delay to allow
                 // the progress indicator to clear)
                 new Timer() {
                  @Override
                  public void run()
                  {
                     server_.installBuildTools(
                           "Managing packages with Packrat",
                           new SimpleRequestCallback<Boolean>() {});  
                  }   
                 }.schedule(250);;
                
              }
           }

         @Override
         public void onError(ServerError error)
         {
            indicator.onError(error.getUserMessage());
         }
        });  
   }

   private void executeBootstrap(final PackratPrerequisites prereqs)
   {
      
      final Command bootstrapCommand = new Command() { 
         @Override
         public void execute()  
         {
            packratUtil_.executePackratFunction("init");
         }
      };

      if (prereqs.getPackageAvailable())
      {
         forceClosed(bootstrapCommand);
      }
      else
      {
         final ProgressIndicator indicator = getProgressIndicator();
         indicator.onProgress("Installing Packrat...");

         server_.installPackrat(new ServerRequestCallback<Boolean>() {

            @Override
            public void onResponseReceived(Boolean success)
            {
               indicator.onCompleted();

               if (success)
                  forceClosed(bootstrapCommand);
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }

         });
      }

   }
   
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final PackratServerOperations server_;
   private final PackratUtil packratUtil_;
   
   private CheckBox chkUsePackrat_;
   private CheckBox chkAutoSnapshot_;
   private CheckBox chkUseCache_;
   private CheckBox chkVcsIgnoreLib_;
   private CheckBox chkVcsIgnoreSrc_;
   
   private VerticalPanel panelExternalPackages_;
   private TextArea taExternalPackages_;
   
   private VerticalPanel panelLocalRepos_;
   private TextArea taLocalRepos_;
   
   private ThemedButton usePackratButton_;   
}
