/*
 * PackagesPreferencesPane.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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


package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.PackagesHelpLink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.repos.SecondaryReposWidget;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.PackagesPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class PackagesPreferencesPane extends PreferencesPane
{
   @Inject
   public PackagesPreferencesPane(PreferencesDialogResources res,
                                  GlobalDisplay globalDisplay,
                                  UIPrefs uiPrefs,
                                  Session session,
                                  final DefaultCRANMirror defaultCRANMirror,
                                  MirrorsServerOperations server)
   {
      res_ = res;
      globalDisplay_ = globalDisplay;
      server_ = server;

      secondaryReposWidget_ = new SecondaryReposWidget();

      VerticalPanel management = new VerticalPanel();
      VerticalPanel development = new VerticalPanel();
    
      management.add(headerLabel("Package Management"));

      infoBar_ = new InfoBar(InfoBar.WARNING);
      infoBar_.setText("CRAN repositories modified outside package preferences.");
      infoBar_.addStyleName(res_.styles().themeInfobar());
      spaced(infoBar_);
      
      cranMirrorTextBox_ = new TextBoxWithButton(
            "Primary CRAN repository:",
            "",
            "Change...",
            null,
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  defaultCRANMirror.choose(new OperationWithInput<CRANMirror>(){
                     @Override
                     public void execute(CRANMirror cranMirror)
                     {
                        cranMirror_ = cranMirror;
                        cranMirrorTextBox_.setText(cranMirror_.getDisplay());
                        
                        if (cranMirror_.getHost().equals("Custom"))
                        {
                           cranMirrorTextBox_.setText(cranMirror_.getURL());
                        }
                        else
                        {
                           cranMirrorTextBox_.setText(cranMirror_.getDisplay());
                        }

                        secondaryReposWidget_.setCranRepoUrl(
                           cranMirror_.getURL(),
                           cranMirror_.getHost().equals("Custom")
                        );
                     }     
                  });
               }
            },
            true);
      
      cranMirrorTextBox_.getTextBox().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (!event.getValue().equals(cranMirror_.getDisplay()))
            {
               secondaryReposWidget_.setCranRepoUrl(event.getValue(), true);
            }
         }
      });

      nudgeRight(cranMirrorTextBox_);
      textBoxWithChooser(cranMirrorTextBox_);
      cranMirrorTextBox_.setText("");

      Label secondaryReposLabel = new Label("Secondary repositories:");
      secondaryReposLabel.getElement().getStyle().setMarginLeft(2, Unit.PX);
      secondaryReposLabel.getElement().getStyle().setMarginBottom(2, Unit.PX);

      if (session.getSessionInfo().getAllowCRANReposEdit())
      {
         management.add(infoBar_);

         lessSpaced(cranMirrorTextBox_);
         management.add(cranMirrorTextBox_);

         management.add(spacedBefore(secondaryReposLabel));
         management.add(secondaryReposWidget_);
      }

      CheckBox chkEnablePackages = checkboxPref("Enable packages pane", 
         uiPrefs.packagesPaneEnabled());

      chkEnablePackages.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            reloadRequired_ = true;
         }
      });

      if (!session.getSessionInfo().getDisablePackages())
      {
         management.add(spacedBefore(chkEnablePackages));
      }
      
      useSecurePackageDownload_ = new CheckBox(
            "Use secure download method for HTTP");
      HorizontalPanel secureDownloadPanel = checkBoxWithHelp(
                        useSecurePackageDownload_, "secure_download", "Help on secure package downloads for R");
      lessSpaced(secureDownloadPanel);
      management.add(secureDownloadPanel);
      
      useInternet2_ = new CheckBox(
                        "Use Internet Explorer library/proxy for HTTP",
                        true);
      if (BrowseCap.isWindowsDesktop())
      {     
         lessSpaced(chkEnablePackages);
         spaced(useInternet2_);
         management.add(useInternet2_);
      }
      else
      {
         spaced(useSecurePackageDownload_);
         useSecurePackageDownload_.getElement().getStyle().setMarginBottom(12, Unit.PX);
      }

      management.add(spacedBefore(new HelpLink("Managing Packages", "managing_packages")));
      
      development.add(headerLabel("Package Development"));
      
      useDevtools_ = new CheckBox("Use devtools package functions if available");
      lessSpaced(useDevtools_);
      development.add(useDevtools_);
      
      development.add(checkboxPref("Save all files prior to building packages", uiPrefs.saveAllBeforeBuild()));
      development.add(checkboxPref("Automatically navigate editor to build errors", uiPrefs.navigateToBuildError()));
      
      hideObjectFiles_ = new CheckBox("Hide object files in package src directory");
      lessSpaced(hideObjectFiles_);
      development.add(hideObjectFiles_);
      
      cleanupAfterCheckSuccess_ = new CheckBox("Cleanup output after successful R CMD check");
      lessSpaced(cleanupAfterCheckSuccess_);
      development.add(cleanupAfterCheckSuccess_);
      
      viewDirAfterCheckFailure_ = new CheckBox("View Rcheck directory after failed R CMD check");
      lessSpaced(viewDirAfterCheckFailure_);
      development.add(viewDirAfterCheckFailure_);
      
      development.add(checkboxPref("Use Rcpp template when creating C++ files", uiPrefs.useRcppTemplate()));
      
      useNewlineInMakefiles_ = new CheckBox("Always use LF line-endings in Unix Makefiles");
      lessSpaced(useNewlineInMakefiles_);
      development.add(useNewlineInMakefiles_);
      
      HelpLink packagesHelpLink = new PackagesHelpLink();
      packagesHelpLink.getElement().getStyle().setMarginTop(12, Unit.PX);
      nudgeRight(packagesHelpLink); 
      development.add(packagesHelpLink);
      
      cranMirrorTextBox_.setEnabled(false);
      useInternet2_.setEnabled(false);
      cleanupAfterCheckSuccess_.setEnabled(false);
      viewDirAfterCheckFailure_.setEnabled(false); 
      hideObjectFiles_.setEnabled(false);
      useDevtools_.setEnabled(false);
      useSecurePackageDownload_.setEnabled(false);

      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel();
      tabPanel.setSize("435px", "498px");
      tabPanel.add(management, "Management");
      tabPanel.add(development, "Development");
      tabPanel.selectTab(0);
      add(tabPanel);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconPackages2x());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Packages";
   }
   
   @Override
   protected void initialize(RPrefs prefs)
   {
      // packages prefs
      PackagesPrefs packagesPrefs = prefs.getPackagesPrefs();
      
      cranMirrorTextBox_.setEnabled(true);
      if (!packagesPrefs.getCRANMirror().isEmpty())
      {
         cranMirror_ = packagesPrefs.getCRANMirror();
         
         secondaryReposWidget_.setCranRepoUrl(
            cranMirror_.getURL(),
            cranMirror_.getHost().equals("Custom")
         );

         if (cranMirror_.getHost().equals("Custom"))
         {
            cranMirrorTextBox_.setText(cranMirror_.getURL());
         }
         else
         {
            cranMirrorTextBox_.setText(cranMirror_.getDisplay());
         }
         
         cranMirrorStored_ = cranMirrorTextBox_.getTextBox().getText();
         
         secondaryReposWidget_.setRepos(cranMirror_.getSecondaryRepos());
      }
      
      useInternet2_.setEnabled(true);
      useInternet2_.setValue(packagesPrefs.getUseInternet2());
      useInternet2_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            globalDisplay_.showMessage(
                  MessageDialog.INFO, 
                  "Restart R Required",
                  "You must restart your R session for this setting " +
                  "to take effect.");
         }
      });
      
      cleanupAfterCheckSuccess_.setEnabled(true);
      cleanupAfterCheckSuccess_.setValue(packagesPrefs.getCleanupAfterCheckSuccess());
      
      viewDirAfterCheckFailure_.setEnabled(true);
      viewDirAfterCheckFailure_.setValue(packagesPrefs.getViewDirAfterCheckFailure());
      
      hideObjectFiles_.setEnabled(true);
      hideObjectFiles_.setValue(packagesPrefs.getHideObjectFiles());
      
      useDevtools_.setEnabled(true);
      useDevtools_.setValue(packagesPrefs.getUseDevtools());
      
      useSecurePackageDownload_.setEnabled(true);
      useSecurePackageDownload_.setValue(packagesPrefs.getUseSecureDownload());
      
      useNewlineInMakefiles_.setEnabled(true);
      useNewlineInMakefiles_.setValue(packagesPrefs.getUseNewlineInMakefiles());

      server_.getCRANActives(
         new SimpleRequestCallback<JsArray<CRANMirror>>() {
            @Override 
            public void onResponseReceived(JsArray<CRANMirror> mirrors)
            {
               boolean cranDiffers = false;

               ArrayList<CRANMirror> secondary = cranMirror_.getSecondaryRepos();

               if (secondary.size() + 1 != mirrors.length() || mirrors.length() == 0)
               {
                  cranDiffers = true;
               }
               else
               {
                  // First entry should always be CRAN when set by preferences
                  if (!mirrors.get(0).getName().equals("CRAN") ||
                      !mirrors.get(0).getURL().equals(cranMirror_.getURL())) {
                     cranDiffers = true;
                  }
                  for(int i=1; i<mirrors.length(); i++)
                  {
                     if (!mirrors.get(i).getName().equals(secondary.get(i-1).getName()) ||
                         !mirrors.get(i).getURL().equals(secondary.get(i-1).getURL()))
                     {
                        cranDiffers = true;
                        break;
                     }
                  }
               }

               if (cranDiffers)
               {
                  infoBar_.addStyleName(res_.styles().themeInfobarShowing());
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         }
      );
   }

   private boolean secondaryReposHasChanged()
   {
      ArrayList<CRANMirror> secondaryRepos = secondaryReposWidget_.getRepos();

      if (secondaryRepos.size() != cranMirror_.getSecondaryRepos().size())
         return true;

      for (int i = 0; i < secondaryRepos.size(); i++)
      {
         if (secondaryRepos.get(i).getSecondary() != cranMirror_.getSecondaryRepos().get(i).getSecondary())
            return true;
      }

      return false;
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean reload = super.onApply(rPrefs);

      String mirrorTextValue = cranMirrorTextBox_.getTextBox().getText();

      if (!mirrorTextValue.equals(cranMirrorStored_))
         cranMirror_.setChanged(true);

      boolean cranRepoChanged = !mirrorTextValue.equals(cranMirrorStored_);
      boolean cranRepoChangedToUrl = cranRepoChanged && 
                                      mirrorTextValue.startsWith("http");
   
      if (cranRepoChanged || secondaryReposHasChanged())
      {
         cranMirror_.setChanged(true);

         if (cranRepoChangedToUrl)
         {
            cranMirror_.setURL(mirrorTextValue);

            cranMirror_.setHost("Custom");
            cranMirror_.setName("Custom");
         }
         
         ArrayList<CRANMirror> repos = secondaryReposWidget_.getRepos();
         cranMirror_.setSecondaryRepos(repos);

         server_.setCRANMirror(
            cranMirror_,
            new SimpleRequestCallback<Void>("Error Setting CRAN Mirror") {
                @Override
                public void onResponseReceived(Void response)
                {
                }
            }
         );
      }
     
      // set packages prefs
      PackagesPrefs packagesPrefs = PackagesPrefs.create(
                                              cranMirror_, 
                                              useInternet2_.getValue(),
                                              null,
                                              cleanupAfterCheckSuccess_.getValue(),
                                              viewDirAfterCheckFailure_.getValue(),
                                              hideObjectFiles_.getValue(),
                                              useDevtools_.getValue(),
                                              useSecurePackageDownload_.getValue(),
                                              useNewlineInMakefiles_.getValue());
      rPrefs.setPackagesPrefs(packagesPrefs);
      
      return reload || reloadRequired_;
   }

   private final PreferencesDialogResources res_;
   private final GlobalDisplay globalDisplay_;
   private final MirrorsServerOperations server_;
   private final InfoBar infoBar_;
   
   private CRANMirror cranMirror_ = CRANMirror.empty();
   private CheckBox useInternet2_;
   private TextBoxWithButton cranMirrorTextBox_;
   private CheckBox cleanupAfterCheckSuccess_;
   private CheckBox viewDirAfterCheckFailure_;
   private CheckBox hideObjectFiles_;
   private CheckBox useDevtools_;
   private CheckBox useSecurePackageDownload_;
   private CheckBox useNewlineInMakefiles_;
   private boolean reloadRequired_ = false;
   private String cranMirrorStored_;
   private SecondaryReposWidget secondaryReposWidget_;
}
