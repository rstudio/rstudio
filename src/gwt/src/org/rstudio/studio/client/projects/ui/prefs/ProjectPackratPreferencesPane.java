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

import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.packrat.PackratUtil;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectPackratOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.source.model.CppCapabilities;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectPackratPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectPackratPreferencesPane(Session session,
                                        PackratServerOperations server,
                                        PackratUtil packratUtil)
   {
      session_ = session;
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
        
        // create the check boxes (we'll add them later if appropriate)
        
        chkModeOn_ = new CheckBox("Packrat mode on (enable private library)");
        chkModeOn_.getElement().getStyle().setMarginTop(10, Unit.PX);
        chkModeOn_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageCheckBoxes();
         }
        });
        
        chkModeOn_.setValue(packratOptions.getModeOn());
        
        chkAutoSnapshot_ = new CheckBox("Automatically snapshot local changes");
        chkAutoSnapshot_.setValue(packratOptions.getAutoSnapshot());
        
        String vcsName = session_.getSessionInfo().getVcsName();
        chkVcsIgnoreLib_ = new CheckBox(vcsName + " ignore packrat library"); 
        chkVcsIgnoreLib_.setValue(packratOptions.getVcsIgnoreLib());
        
        chkVcsIgnoreSrc_ = new CheckBox(vcsName + " ignore packrat sources");
        chkVcsIgnoreSrc_.setValue(packratOptions.getVcsIgnoreSrc());
        
        manageCheckBoxes();
        
        if (!context.isPackified())
        {
           ThemedButton button = new ThemedButton(
              "Use Packrat with this Project",
              new ClickHandler() {
   
                 @Override
                 public void onClick(ClickEvent event)
                 {
                    bootstrapPackrat();
                 }
                 
              });
           spaced(button);
           button.getElement().getStyle().setMarginTop(10, Unit.PX);
           add(button);
        }
        else
        {
           spaced(chkModeOn_);
           add(chkModeOn_);

           spaced(chkAutoSnapshot_);
           add(chkAutoSnapshot_);
           
           spaced(chkVcsIgnoreLib_);
           add(chkVcsIgnoreLib_);
           
           spaced(chkVcsIgnoreSrc_);
           add(chkVcsIgnoreSrc_);
        }
        

        HelpLink helpLink = new HelpLink("Learn more about Packrat", "packrat");
        helpLink.getElement().getStyle().setMarginTop(15, Unit.PX);
        nudgeRight(helpLink);
        add(helpLink);
   }
   
   private void manageCheckBoxes()
   {
      boolean modeOn = chkModeOn_.getValue();
      boolean vcsActive = !session_.getSessionInfo().getVcsName().equals("");
      
      chkAutoSnapshot_.setVisible(modeOn);
      chkVcsIgnoreLib_.setVisible(modeOn && vcsActive);
      chkVcsIgnoreSrc_.setVisible(modeOn && vcsActive);
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectPackratOptions packratOptions = options.getPackratOptions();
      packratOptions.setModeOn(chkModeOn_.getValue());
      packratOptions.setAutoSnapshot(chkAutoSnapshot_.getValue());
      packratOptions.setVcsIgnoreLib(chkVcsIgnoreLib_.getValue());
      packratOptions.setVcsIgnoreSrc(chkVcsIgnoreSrc_.getValue());
      return false;
   }
  
   private void bootstrapPackrat()
   {
      final ProgressIndicator indicator = getProgressIndicator();
      
      indicator.onProgress("Verifying prequisites...");
      
      server_.getCppCapabilities(
        new ServerRequestCallback<CppCapabilities>() {
           @Override
           public void onResponseReceived(CppCapabilities capabilities)
           {
              indicator.onCompleted();
              
              if (capabilities.getCanBuild())
              {
                 executeBootstrap();
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

   private void executeBootstrap()
   {
      forceClosed(new Command() { public void execute()
      {
         packratUtil_.executePackratFunction("bootstrap");
      }});
   }
   
   private final Session session_;
   private final PackratServerOperations server_;
   private final PackratUtil packratUtil_;
   
   private CheckBox chkModeOn_;
   private CheckBox chkAutoSnapshot_;
   private CheckBox chkVcsIgnoreLib_;
   private CheckBox chkVcsIgnoreSrc_;
   
}
