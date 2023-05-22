/*
 * PackagesPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.PackagesHelpLink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.repos.SecondaryReposWidget;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.projects.RenvContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.inject.Inject;

public class PackagesPreferencesPane extends PreferencesPane
{
   @Inject
   public PackagesPreferencesPane(PreferencesDialogResources res,
                                  GlobalDisplay globalDisplay,
                                  UserPrefs uiPrefs,
                                  Session session,
                                  final DefaultCRANMirror defaultCRANMirror,
                                  MirrorsServerOperations server)
   {
      res_ = res;
      globalDisplay_ = globalDisplay;
      server_ = server;

      secondaryReposWidget_ = new SecondaryReposWidget();

      VerticalTabPanel management = new VerticalTabPanel(ElementIds.PACKAGE_MANAGEMENT_PREFS);
      VerticalTabPanel development = new VerticalTabPanel(ElementIds.PACKAGE_DEVELOPMENT_PREFS);
      VerticalTabPanel cpp = new VerticalTabPanel(ElementIds.PACKAGE_CPP_PREFS);
      
      // -------- Management
      management.add(headerLabel(constants_.packageManagementTitle()));

      infoBar_ = new InfoBar(InfoBar.WARNING);
      infoBar_.setText(constants_.packagesInfoBarText());
      infoBar_.addStyleName(res_.styles().themeInfobar());
      infoBar_.getElement().setId(ElementIds.PACKAGE_INFO_BAR);
      spaced(infoBar_);

      ClickHandler selectPrimaryRepo = (clickEvent) ->
      {
         defaultCRANMirror.choose(cranMirror ->
         {
            cranMirror_ = cranMirror;
            cranMirrorTextBox_.setText(cranMirror_.getDisplay());

            secondaryReposWidget_.setCranRepoUrl(
                  cranMirror_.getURL(),
                  cranMirror_.isCustom()
            );
         });
      };

      cranMirrorTextBox_ = new TextBoxWithButton(
            constants_.cranMirrorTextBoxTitle(),
            "",
            constants_.cranMirrorChangeLabel(),
            null,
            ElementIds.TextBoxButtonId.PRIMARY_CRAN,
            true,
            selectPrimaryRepo);

      cranMirrorTextBox_.getTextBox().addValueChangeHandler(event ->
      {
         if (!event.getValue().equals(cranMirror_.getDisplay()))
         {
            secondaryReposWidget_.setCranRepoUrl(event.getValue(), true);
         }
      });

      nudgeRight(cranMirrorTextBox_);
      textBoxWithChooser(cranMirrorTextBox_);
      cranMirrorTextBox_.setText("");

      if (session.getSessionInfo().getAllowCRANReposEdit())
      {
         management.add(infoBar_);

         lessSpaced(cranMirrorTextBox_);
         management.add(cranMirrorTextBox_);

         FormLabel secondaryReposLabel = new FormLabel(
               constants_.secondaryReposTitle(),
               secondaryReposWidget_.getLabeledWidget());
         secondaryReposLabel.getElement().getStyle().setMarginLeft(2, Unit.PX);
         secondaryReposLabel.getElement().getStyle().setMarginBottom(2, Unit.PX);

         management.add(spacedBefore(secondaryReposLabel));
         management.add(secondaryReposWidget_);
      }

      CheckBox chkEnablePackages = checkboxPref(constants_.chkEnablePackagesTitle(),
         uiPrefs.packagesPaneEnabled());

      chkEnablePackages.addValueChangeHandler(event -> reloadRequired_ = true);

      if (!session.getSessionInfo().getDisablePackages())
      {
         management.add(spacedBefore(chkEnablePackages));
      }

      useSecurePackageDownload_ = new CheckBox(
              constants_.useSecurePackageDownloadTitle());
      HorizontalPanel secureDownloadPanel = HelpButton.checkBoxWithHelp(
                        useSecurePackageDownload_,
                        new HelpButton("secure_download", constants_.useSecurePackageTitle()));
      lessSpaced(secureDownloadPanel);
      management.add(secureDownloadPanel);

      useInternet2_ = new CheckBox(
                        constants_.useInternetTitle(),
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

      management.add(spacedBefore(new HelpLink(constants_.managePackagesTitle(), "managing_packages")));

      // -------- Development
      development.add(headerLabel(constants_.developmentTitle()));

      useDevtools_ = new CheckBox(constants_.useDevtoolsLabel());
      lessSpaced(useDevtools_);
      development.add(useDevtools_);

      development.add(checkboxPref(constants_.developmentSaveLabel(), uiPrefs.saveFilesBeforeBuild()));
      development.add(checkboxPref(constants_.developmentNavigateLabel(), uiPrefs.navigateToBuildError()));

      cleanupAfterCheckSuccess_ = new CheckBox(constants_.developmentCleanupLabel());
      lessSpaced(cleanupAfterCheckSuccess_);
      development.add(cleanupAfterCheckSuccess_);

      viewDirAfterCheckFailure_ = new CheckBox(constants_.developmentViewLabel());
      lessSpaced(viewDirAfterCheckFailure_);
      development.add(viewDirAfterCheckFailure_);

      useNewlineInMakefiles_ = new CheckBox(constants_.developmentUseLFLabel());
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
      useDevtools_.setEnabled(false);
      useSecurePackageDownload_.setEnabled(false);

      // -------- R / C++
      cpp.add(headerLabel(constants_.cppDevelopmentTitle()));

      cppTemplate_ = new SelectWidget(
         constants_.developmentCppTemplate(),
         new String[] {
               "Rcpp", 
               "cpp11", 
               constants_.developmentEmptyLabel()
         },
         new String[] {
            "Rcpp", "cpp11", "empty"
         },
         false,
         true,
         false);
      cpp.add(cppTemplate_);
     
      hideObjectFiles_ = new CheckBox(constants_.developmentHideLabel());
      lessSpaced(hideObjectFiles_);
      cpp.add(hideObjectFiles_);
      hideObjectFiles_.setEnabled(false);
      
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel(constants_.tabPackagesPanelTitle());
      setTabPanelSize(tabPanel);
      tabPanel.add(management, constants_.managementPanelTitle(), management.getBasePanelId());
      tabPanel.add(development, constants_.developmentManagementPanelTitle(), development.getBasePanelId());
      tabPanel.add(cpp, constants_.cppPanelTitle(), cpp.getBasePanelId());
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
      return constants_.tabPackagesPanelTitle();
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      cranMirrorTextBox_.setEnabled(true);
      CRANMirror mirror = prefs.cranMirror().getValue().cast();
      if (!mirror.isEmpty())
      {
         cranMirror_ = mirror;

         secondaryReposWidget_.setCranRepoUrl(
            cranMirror_.getURL(),
            cranMirror_.isCustom()
         );

         cranMirrorTextBox_.setText(cranMirror_.getDisplay());

         cranMirrorStored_ = cranMirrorTextBox_.getTextBox().getText();

         secondaryReposWidget_.setRepos(cranMirror_.getSecondaryRepos());
      }

      useInternet2_.setEnabled(true);
      useInternet2_.setValue(prefs.useInternet2().getValue());
      useInternet2_.addValueChangeHandler(event -> globalDisplay_.showMessage(
            MessageDialog.INFO,
            constants_.cranMirrorTextBoxRestartCaption(),
            constants_.cranMirrorTextBoxRestartMessage())
      );

      cleanupAfterCheckSuccess_.setEnabled(true);
      cleanupAfterCheckSuccess_.setValue(prefs.cleanupAfterRCmdCheck().getValue());

      viewDirAfterCheckFailure_.setEnabled(true);
      viewDirAfterCheckFailure_.setValue(prefs.viewDirAfterRCmdCheck().getValue());

      hideObjectFiles_.setEnabled(true);
      hideObjectFiles_.setValue(prefs.hideObjectFiles().getValue());

      useDevtools_.setEnabled(true);
      useDevtools_.setValue(prefs.useDevtools().getValue());

      useSecurePackageDownload_.setEnabled(true);
      useSecurePackageDownload_.setValue(prefs.useSecureDownload().getValue());

      useNewlineInMakefiles_.setEnabled(true);
      useNewlineInMakefiles_.setValue(prefs.useNewlinesInMakefiles().getValue());

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
   
      // If renv is managing our repos, make the info bar more specific
      server_.readProjectOptions(
         new SimpleRequestCallback<RProjectOptions>() {
            @Override
            public void onResponseReceived(RProjectOptions rProjectOptions) {
               RenvContext renvCtx = rProjectOptions.getRenvContext();
               if (renvCtx.active) {
                  infoBar_.setText(constants_.packagesRenvInfoBarText());
                  infoBar_.addStyleName(res_.styles().themeInfobarShowing());
               }
            }
         
            @Override
            public void onError(ServerError error) {
               Debug.logError(error);
            }
         }
      );

      cppTemplate_.setValue(prefs.cppTemplate().getValue());
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
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      if (reloadRequired_)
         restartRequirement.setUiReloadRequired(true);

      String mirrorTextValue = cranMirrorTextBox_.getTextBox().getText();

      boolean cranRepoChanged = !mirrorTextValue.equals(cranMirrorStored_);
      boolean cranRepoChangedToUrl = cranRepoChanged &&
                                      mirrorTextValue.startsWith("http");

      if (cranRepoChanged || secondaryReposHasChanged())
      {
         if (cranRepoChangedToUrl)
         {
            cranMirror_.setURL(mirrorTextValue);

            cranMirror_.setAsCustom();
         }
      }

      ArrayList<CRANMirror> repos = secondaryReposWidget_.getRepos();
      cranMirror_.setSecondaryRepos(repos);

      prefs.cranMirror().setGlobalValue(cranMirror_);
      prefs.useInternet2().setGlobalValue(useInternet2_.getValue());
      prefs.cleanupAfterRCmdCheck().setGlobalValue(cleanupAfterCheckSuccess_.getValue());
      prefs.viewDirAfterRCmdCheck().setGlobalValue(viewDirAfterCheckFailure_.getValue());
      prefs.hideObjectFiles().setGlobalValue(hideObjectFiles_.getValue());
      prefs.useDevtools().setGlobalValue(useDevtools_.getValue());
      prefs.useSecureDownload().setGlobalValue(useSecurePackageDownload_.getValue());
      prefs.useNewlinesInMakefiles().setGlobalValue(useNewlineInMakefiles_.getValue());
      prefs.cranMirror().setGlobalValue(cranMirror_);
      prefs.cppTemplate().setGlobalValue(cppTemplate_.getValue());
      return restartRequirement;
   }

   private final PreferencesDialogResources res_;
   private final GlobalDisplay globalDisplay_;
   private final MirrorsServerOperations server_;
   private final InfoBar infoBar_;

   private CRANMirror cranMirror_ = CRANMirror.empty();
   private final CheckBox useInternet2_;
   private TextBoxWithButton cranMirrorTextBox_;
   private final CheckBox cleanupAfterCheckSuccess_;
   private final CheckBox viewDirAfterCheckFailure_;
   private final CheckBox hideObjectFiles_;
   private final CheckBox useDevtools_;
   private final CheckBox useSecurePackageDownload_;
   private final CheckBox useNewlineInMakefiles_;
   private final SelectWidget cppTemplate_;
   
   private boolean reloadRequired_ = false;
   private String cranMirrorStored_;
   private final SecondaryReposWidget secondaryReposWidget_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
