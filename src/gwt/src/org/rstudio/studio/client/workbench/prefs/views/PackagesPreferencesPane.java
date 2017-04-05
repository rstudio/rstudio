/*
 * PackagesPreferencesPane.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.PackagesHelpLink;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
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
                                  final DefaultCRANMirror defaultCRANMirror)
   {
      res_ = res;
      globalDisplay_ = globalDisplay;
    
      add(headerLabel("Package management"));
      
      cranMirrorTextBox_ = new TextBoxWithButton(
            "CRAN mirror:",
            "Change...",
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
                     }     
                  });
                 
               }
            });
      nudgeRight(cranMirrorTextBox_);
      textBoxWithChooser(cranMirrorTextBox_);
      cranMirrorTextBox_.setText("");
      if (session.getSessionInfo().getAllowCRANReposEdit())
      {
         lessSpaced(cranMirrorTextBox_);
         add(cranMirrorTextBox_);
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
         add(chkEnablePackages);
      }
      
      useSecurePackageDownload_ = new CheckBox(
            "Use secure download method for HTTP");
      HorizontalPanel secureDownloadPanel = checkBoxWithHelp(
                        useSecurePackageDownload_, "secure_download");
      lessSpaced(secureDownloadPanel);
      add(secureDownloadPanel);
      
      useInternet2_ = new CheckBox(
                        "Use Internet Explorer library/proxy for HTTP",
                        true);
      if (BrowseCap.isWindowsDesktop())
      {     
         lessSpaced(chkEnablePackages);
         spaced(useInternet2_);
         add(useInternet2_);
      }
      else
      {
         spaced(useSecurePackageDownload_);
         useSecurePackageDownload_.getElement().getStyle().setMarginBottom(12, Unit.PX);
      }
      
      add(headerLabel("Package development"));
      
      useDevtools_ = new CheckBox("Use devtools package functions if available");
      lessSpaced(useDevtools_);
      add(useDevtools_);
      
      add(checkboxPref("Save all files prior to building packages", uiPrefs.saveAllBeforeBuild()));
      add(checkboxPref("Automatically navigate editor to build errors", uiPrefs.navigateToBuildError()));
      
      hideObjectFiles_ = new CheckBox("Hide object files in package src directory");
      lessSpaced(hideObjectFiles_);
      add(hideObjectFiles_);
      
      cleanupAfterCheckSuccess_ = new CheckBox("Cleanup output after successful R CMD check");
      lessSpaced(cleanupAfterCheckSuccess_);
      add(cleanupAfterCheckSuccess_);
      
      viewDirAfterCheckFailure_ = new CheckBox("View Rcheck directory after failed R CMD check");
      lessSpaced(viewDirAfterCheckFailure_);
      add(viewDirAfterCheckFailure_);
      
      add(checkboxPref("Use Rcpp template when creating C++ files", uiPrefs.useRcppTemplate()));
      
      useNewlineInMakefiles_ = new CheckBox("Always use LF line-endings in Unix Makefiles");
      lessSpaced(useNewlineInMakefiles_);
      add(useNewlineInMakefiles_);
      
      HelpLink packagesHelpLink = new PackagesHelpLink();
      packagesHelpLink.getElement().getStyle().setMarginTop(12, Unit.PX);
      nudgeRight(packagesHelpLink); 
      add(packagesHelpLink);
      
      cranMirrorTextBox_.setEnabled(false);
      useInternet2_.setEnabled(false);
      cleanupAfterCheckSuccess_.setEnabled(false);
      viewDirAfterCheckFailure_.setEnabled(false); 
      hideObjectFiles_.setEnabled(false);
      useDevtools_.setEnabled(false);
      useSecurePackageDownload_.setEnabled(false);
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
         cranMirrorTextBox_.setText(cranMirror_.getDisplay());
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
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean reload = super.onApply(rPrefs);
     
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
}
