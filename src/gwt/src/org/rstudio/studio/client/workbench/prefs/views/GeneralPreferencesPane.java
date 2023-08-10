/*
 * GeneralPreferencesPane.java
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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.ui.RVersionSelectWidget;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.LocaleCookie;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.WebDialogCookie;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class GeneralPreferencesPane extends PreferencesPane
{
   private final static PrefsConstants constants_ = GWT.create(PrefsConstants.class);

   @Inject
   public GeneralPreferencesPane(RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 UserPrefs prefs,
                                 Session session,
                                 GlobalDisplay globalDisplay,
                                 WorkbenchContext context)
   {
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      prefs_ = prefs;
      session_ = session;

      RVersionsInfo versionsInfo = context.getRVersionsInfo();
      VerticalTabPanel basic = new VerticalTabPanel(ElementIds.GENERAL_BASIC_PREFS);

      basic.add(headerLabel(constants_.rSessionsTitle()));
      if (BrowseCap.isWindowsDesktop())
      {
         rVersion_ = new TextBoxWithButton(
               constants_.rVersionTitle(),
               "",
               constants_.rVersionChangeTitle(),
               null,
               ElementIds.TextBoxButtonId.R_VERSION,
               true,
               new ClickHandler()
               {
                  @Override
                  public void onClick(ClickEvent event)
                  {
                     Desktop.getFrame().chooseRVersion(ver ->
                     {
                        if (!StringUtil.isNullOrEmpty(ver))
                        {
                           rVersion_.setText(ver);

                           globalDisplay.showMessage(MessageDialog.INFO,
                                   constants_.rChangeVersionMessage(),
                                   constants_.rQuitReOpenMessage());
                        }
                     });
                  }
               });
         rVersion_.setWidth("100%");
         rVersion_.setText(constants_.rVersionLoadingText());
         rVersion_.getElement().getStyle().setMarginLeft(2, Unit.PX);
         Desktop.getFrame().getRVersion(version -> {
            rVersion_.setText(version);
         });
         spaced(rVersion_);
         basic.add(rVersion_);
      }
      if (versionsInfo.isMultiVersion())
      {
         rServerRVersion_ = new RVersionSelectWidget(
                                       ElementIds.SelectWidgetId.R_VER_GEN_PREF_PANE,
                                       versionsInfo.getAvailableRVersions());
         basic.add(tight(rServerRVersion_));

         rememberRVersionForProjects_ =
                        new CheckBox(constants_.rRestoreLabel());

         rememberRVersionForProjects_.setValue(true);
         Style style = rememberRVersionForProjects_.getElement().getStyle();
         style.setMarginTop(5, Unit.PX);
         style.setMarginBottom(12, Unit.PX);
         basic.add(rememberRVersionForProjects_);
      }

      dirChooser_ = new DirectoryChooserTextBox(
            constants_.rDefaultDirectoryTitle(),
            ElementIds.TextBoxButtonId.DEFAULT_WORKING_DIR,
            null,
            fileDialogs_,
            fsContext_);
      
      spaced(dirChooser_);
      nudgeRight(dirChooser_);
      textBoxWithChooser(dirChooser_);
      basic.add(dirChooser_);

      restoreLastProject_ = new CheckBox(constants_.rRestorePreviousTitle());
      lessSpaced(restoreLastProject_);
      basic.add(restoreLastProject_);

        basic.add(checkboxPref(constants_.rRestorePreviousOpenTitle(), prefs_.restoreSourceDocuments()));

      rProfileOnResume_ = new CheckBox(constants_.rRunProfileTitle());
      if (!Desktop.isDesktop())
         basic.add(rProfileOnResume_);

      basic.add(spacedBefore(headerLabel(constants_.workspaceCaption())));
      basic.add(loadRData_ = new CheckBox(constants_.workspaceLabel()));
      lessSpaced(loadRData_);

      saveWorkspace_ = new SelectWidget(
              constants_.saveWorkSpaceLabel(),
            new String[]
            {
               constants_.saveWorkAlways(),
               constants_.saveWorkNever(),
               constants_.saveWorkAsk()
            },
            new String[]
            {
               UserPrefs.SAVE_WORKSPACE_ALWAYS,
               UserPrefs.SAVE_WORKSPACE_NEVER,
               UserPrefs.SAVE_WORKSPACE_ASK
            }, false, true, false);
      spaced(saveWorkspace_);
      basic.add(saveWorkspace_);

      basic.add(headerLabel(constants_.historyCaption()));
      alwaysSaveHistory_ = new CheckBox(
              constants_.alwaysSaveHistoryLabel());
      lessSpaced(alwaysSaveHistory_);
      basic.add(alwaysSaveHistory_);

      removeHistoryDuplicates_ = new CheckBox(
                                constants_.removeDuplicatesLabel());
      basic.add(removeHistoryDuplicates_);

      basic.add(spacedBefore(headerLabel(constants_.otherCaption())));

      basic.add(checkboxPref(
            constants_.otherWrapAroundLabel(),
            prefs_.wrapTabNavigation(),
            true /*defaultSpaced*/));

      // provide check for updates option in desktop mode when not
      // already globally disabled
      if (Desktop.isDesktop() &&
          !session.getSessionInfo().getDisableCheckForUpdates())
      {
         basic.add(checkboxPref(constants_.otherNotifyMeLabel(),
                   prefs_.checkForUpdates(), true /*defaultSpaced*/));
      }

      // crash reporting - only show in desktop mode
      enableCrashReporting_ = checkboxPref(constants_.otherSendReportsLabel(),
            prefs_.submitCrashReports());
      if (Desktop.isDesktop())
      {
         lessSpaced(enableCrashReporting_);
         enableCrashReporting_.setEnabled(session.getSessionInfo().getCrashHandlerSettingsModifiable());
         basic.add(enableCrashReporting_);
      }

      VerticalTabPanel graphics = new VerticalTabPanel(ElementIds.GENERAL_GRAPHICS_PREFS);

      initializeGraphicsBackendWidget();
      graphics.add(headerLabel(constants_.graphicsDeviceCaption()));
      graphics.add(graphicsBackend_);

      graphicsAntialias_ = new SelectWidget(
            constants_.graphicsAntialiasingLabel(),
            new String[] {
                  constants_.antialiasingDefaultOption(),
                  constants_.antialiasingNoneOption(),
                  constants_.antialiasingGrayOption(),
                  constants_.antialiasingSubpixelOption()
            },
            new String[] {
                  UserPrefs.GRAPHICS_ANTIALIASING_DEFAULT,
                  UserPrefs.GRAPHICS_ANTIALIASING_NONE,
                  UserPrefs.GRAPHICS_ANTIALIASING_GRAY,
                  UserPrefs.GRAPHICS_ANTIALIASING_SUBPIXEL
            },
            false,
            true,
            false);

      graphics.add(graphicsAntialias_);

      VerticalTabPanel advanced = new VerticalTabPanel(ElementIds.GENERAL_ADVANCED_PREFS);

      showServerHomePage_ = new SelectWidget(
              constants_.serverHomePageLabel(),
            new String[] {
                  constants_.serverHomePageActiveSessionsOption(),
                  constants_.serverHomePageAlwaysOption(),
                  constants_.serverHomePageNeverOption()
            },
            new String[] {
                      "sessions",
                      "always",
                      "never"
            },
            false,
            true,
            false);

      reuseSessionsForProjectLinks_ = new CheckBox(constants_.reUseIdleSessionLabel());
      lessSpaced(reuseSessionsForProjectLinks_);
      boolean firstHeader = true;

      if (!Desktop.hasDesktopFrame())
      {
         if (session_.getSessionInfo().getShowUserHomePage() ||
             session_.getSessionInfo().getMultiSession())
         {
            Label homePageLabel = headerLabel(constants_.desktopCaption());
            spacedBefore(homePageLabel);
            advanced.add(homePageLabel);
            firstHeader = false;
         }
         if (session_.getSessionInfo().getShowUserHomePage())
         {
            tight(showServerHomePage_);
            advanced.add(showServerHomePage_);
         }
         if (session_.getSessionInfo().getMultiSession())
            advanced.add(reuseSessionsForProjectLinks_);
      }

     Label debuggingLabel = headerLabel(constants_.advancedDebuggingCaption());
     if (!firstHeader)
     {
        spacedBefore(debuggingLabel);
        firstHeader = false;
     }
     advanced.add(debuggingLabel);
     advanced.add(checkboxPref(
           constants_.advancedDebuggingLabel(),
           prefs_.handleErrorsInUserCodeOnly()));

      if (Desktop.hasDesktopFrame())
      {
         Label osLabel = headerLabel(constants_.advancedOsIntegrationCaption());
         spacedBefore(osLabel);
         advanced.add(osLabel);

         renderingEngineWidget_ = new SelectWidget(constants_.advancedRenderingEngineLabel(), new String[] {});
         renderingEngineWidget_.addChoice(constants_.renderingEngineAutoDetectOption(), ENGINE_AUTO);
         renderingEngineWidget_.addChoice(constants_.renderingEngineDesktopOption(), ENGINE_DESKTOP);
         if (BrowseCap.isLinuxDesktop())
         {
            renderingEngineWidget_.addChoice(constants_.renderingEngineLinuxDesktopOption(), ENGINE_GLES);
         }
         renderingEngineWidget_.addChoice(constants_.renderingEngineSoftwareOption(), ENGINE_SOFTWARE);
         advanced.add(spaced(renderingEngineWidget_));

         Desktop.getFrame().desktopRenderingEngine((String engine) -> {
            if (StringUtil.isNullOrEmpty(engine))
               return;
            renderingEngineWidget_.setValue(engine);
            renderingEngine_ = engine;
         });

         useGpuExclusions_ = new CheckBox(constants_.useGpuExclusionListLabel());
         advanced.add(lessSpaced(useGpuExclusions_));
         Desktop.getFrame().getIgnoreGpuExclusionList((Boolean ignore) -> {
            desktopIgnoreGpuExclusions_ = ignore;
            useGpuExclusions_.setValue(!ignore);
         });

         useGpuDriverBugWorkarounds_ = new CheckBox(constants_.useGpuDriverBugWorkaroundsLabel());
         advanced.add(lessSpaced(useGpuDriverBugWorkarounds_));
         Desktop.getFrame().getDisableGpuDriverBugWorkarounds((Boolean disable) -> {
            desktopDisableGpuDriverBugWorkarounds_ = disable;
            useGpuDriverBugWorkarounds_.setValue(!disable);
         });

         if (BrowseCap.isLinuxDesktop() && !BrowseCap.isElectron())
         {
            clipboardMonitoring_ = new CheckBox(constants_.clipboardMonitoringLabel());
            advanced.add(lessSpaced(clipboardMonitoring_));
            Desktop.getFrame().getClipboardMonitoring(monitoring ->
            {
               desktopMonitoring_ = monitoring;
               clipboardMonitoring_.setValue(monitoring);
            });
         }

         fullPathInTitle_ = new CheckBox(constants_.fullProjectPathInWindowTitleLabel());
         advanced.add(lessSpaced(fullPathInTitle_));
         if (BrowseCap.isElectron())
         {
            nativeFileDialogs_ = checkboxPref(prefs_.nativeFileDialogs());
            advanced.add(nativeFileDialogs_);
            disableRendererAccessibility_ = checkboxPref(prefs_.disableRendererAccessibility());
            advanced.add(disableRendererAccessibility_);
         }
      }

      Label otherLabel = headerLabel(constants_.otherLabel());
      spacedBefore(otherLabel);
      advanced.add(otherLabel);

      showLastDotValue_ = new CheckBox(constants_.otherShowLastDotValueLabel());
      lessSpaced(showLastDotValue_);
      advanced.add(showLastDotValue_);

      String[] labels = {"7", "8", "9", "10", "11", "12", "13", "14", "16", "18", "24", "36"};
      String[] values = new String[labels.length];
      for (int i = 0; i < labels.length; i++)
         values[i] = Double.parseDouble(labels[i]) + "";

      Label experimentalLabel = headerLabel(constants_.experimentalLabel());
      spacedBefore(experimentalLabel);
      advanced.add(experimentalLabel);

      String[] langLabels = {
         constants_.englishLabel(),
         constants_.frenchLabel()
      };
      String[] langValues = {
         UserPrefsAccessor.UI_LANGUAGE_EN,
         UserPrefsAccessor.UI_LANGUAGE_FR
      };
      uiLanguage_ = new SelectWidget(prefs_.uiLanguage().getTitle(),
         langLabels,
         langValues,
         false, /* Multi select */
         true, /* Horizontal label */
         false /* List on left */);
      if (!uiLanguage_.setValue(prefs_.uiLanguage().getValue()))
         uiLanguage_.getListBox().setSelectedIndex(0);
      advanced.add(uiLanguage_);

      showServerHomePage_.setEnabled(false);
      reuseSessionsForProjectLinks_.setEnabled(false);
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
      rProfileOnResume_.setEnabled(false);
      showLastDotValue_.setEnabled(false);
      restoreLastProject_.setEnabled(false);

      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel(constants_.generalTablistLabel());
      setTabPanelSize(tabPanel);
      tabPanel.add(basic, constants_.generalTablListBasicOption(), basic.getBasePanelId());
      tabPanel.add(graphics, constants_.generalTablListGraphicsOption(), graphics.getBasePanelId());
      tabPanel.add(advanced, constants_.generalTabListAdvancedOption(), advanced.getBasePanelId());
      tabPanel.selectTab(0);
      add(tabPanel);
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      boolean isLauncherSession = session_.getSessionInfo().getLauncherSession();
      showServerHomePage_.setEnabled(!isLauncherSession);

      reuseSessionsForProjectLinks_.setEnabled(true);
      saveWorkspace_.setEnabled(true);
      loadRData_.setEnabled(true);
      dirChooser_.setEnabled(true);

      if (!isLauncherSession)
         showServerHomePage_.setValue(prefs.showUserHomePage().getValue());
      else
         showServerHomePage_.setValue(UserPrefs.SHOW_USER_HOME_PAGE_ALWAYS);

      reuseSessionsForProjectLinks_.setValue(prefs.reuseSessionsForProjectLinks().getValue());

      int saveWorkspaceIndex;
      switch (prefs.saveWorkspace().getValue())
      {
         case UserPrefs.SAVE_WORKSPACE_NEVER:
            saveWorkspaceIndex = 1;
            break;
         case UserPrefs.SAVE_WORKSPACE_ALWAYS:
            saveWorkspaceIndex = 0;
            break;
         case UserPrefs.SAVE_WORKSPACE_ASK:
         default:
            saveWorkspaceIndex = 2;
            break;
      }
      saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

      loadRData_.setValue(prefs.loadWorkspace().getValue());
      
      // NOTE: we intentionally ignore sessionInfo's version of the
      // initial working directory here as that might reference a
      // project-specific location, and we don't want that to end up
      // encoded as part of the user's global preferences
      String workingDir = prefs.initialWorkingDirectory().getValue();
      if (StringUtil.isNullOrEmpty(workingDir))
         workingDir = "~";
      
      dirChooser_.setText(workingDir);

      alwaysSaveHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);

      alwaysSaveHistory_.setValue(prefs.alwaysSaveHistory().getValue());
      removeHistoryDuplicates_.setValue(prefs.removeHistoryDuplicates().getValue());

      rProfileOnResume_.setValue(prefs.runRprofileOnResume().getValue());
      rProfileOnResume_.setEnabled(true);

      showLastDotValue_.setValue(prefs.showLastDotValue().getValue());
      showLastDotValue_.setEnabled(true);

      if (rServerRVersion_ != null)
         rServerRVersion_.setRVersion(prefs.defaultRVersion().getValue().cast());

      if (rememberRVersionForProjects_ != null)
      {
         rememberRVersionForProjects_.setValue(
                                   prefs.restoreProjectRVersion().getValue());
      }

      if (fullPathInTitle_ != null)
         fullPathInTitle_.setValue(prefs.fullProjectPathInWindowTitle().getValue());

      enableCrashReporting_.setValue(prefs.submitCrashReports().getValue());

      // projects prefs
      restoreLastProject_.setEnabled(true);
      restoreLastProject_.setValue(prefs.restoreLastProject().getValue());

      // graphics prefs
      graphicsBackend_.setValue(prefs.graphicsBackend().getValue());
      graphicsAntialias_.setValue(prefs.graphicsAntialiasing().getValue());

      initialUiLanguage_ = prefs_.uiLanguage().getValue();
      initialNativeFileDialogs_ = prefs_.nativeFileDialogs().getValue();
      initialDisableRendererAccessibility_ = prefs_.disableRendererAccessibility().getValue();
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconR2x());
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      if (clipboardMonitoring_ != null &&
          desktopMonitoring_ != clipboardMonitoring_.getValue())
      {
         // set monitoring property if changed
         restartRequirement.setDesktopRestartRequired(true);
         boolean desktopMonitoring = clipboardMonitoring_.getValue();
         desktopMonitoring_ = desktopMonitoring;
         Desktop.getFrame().setClipboardMonitoring(desktopMonitoring);
      }

      if (fullPathInTitle_ != null &&
         fullPathInTitle_.getValue() != prefs.fullProjectPathInWindowTitle().getValue())
      {
         restartRequirement.setDesktopRestartRequired(true);
         prefs.fullProjectPathInWindowTitle().setGlobalValue(fullPathInTitle_.getValue());
      }

      if (renderingEngineWidget_ != null &&
          !StringUtil.equals(renderingEngineWidget_.getValue(), renderingEngine_))
      {
         // set desktop renderer when changed
         restartRequirement.setDesktopRestartRequired(true);
         String renderingEngine = renderingEngineWidget_.getValue();
         renderingEngine_ = renderingEngine;
         Desktop.getFrame().setDesktopRenderingEngine(renderingEngine);
      }

      if (useGpuExclusions_ != null &&
          desktopIgnoreGpuExclusions_ != !useGpuExclusions_.getValue())
      {
         restartRequirement.setDesktopRestartRequired(true);
         boolean ignore = !useGpuExclusions_.getValue();
         desktopIgnoreGpuExclusions_ = ignore;
         Desktop.getFrame().setIgnoreGpuExclusionList(ignore);
      }

      if (useGpuDriverBugWorkarounds_ != null &&
          desktopDisableGpuDriverBugWorkarounds_ != !useGpuDriverBugWorkarounds_.getValue())
      {
         restartRequirement.setDesktopRestartRequired(true);
         boolean disable = !useGpuDriverBugWorkarounds_.getValue();
         desktopDisableGpuDriverBugWorkarounds_ = disable;
         Desktop.getFrame().setDisableGpuDriverBugWorkarounds(disable);
      }

      if (saveWorkspace_.isEnabled())
      {
         prefs.saveWorkspace().setGlobalValue(saveWorkspace_.getValue());
      }

      prefs.loadWorkspace().setGlobalValue(loadRData_.getValue());
      prefs.runRprofileOnResume().setGlobalValue(rProfileOnResume_.getValue());
      prefs.initialWorkingDirectory().setGlobalValue(dirChooser_.getText());
      prefs.showLastDotValue().setGlobalValue(showLastDotValue_.getValue());
      prefs.alwaysSaveHistory().setGlobalValue(alwaysSaveHistory_.getValue());
      prefs.removeHistoryDuplicates().setGlobalValue(removeHistoryDuplicates_.getValue());
      prefs.restoreLastProject().setGlobalValue(restoreLastProject_.getValue());
      prefs.graphicsBackend().setGlobalValue(graphicsBackend_.getValue());
      prefs.graphicsAntialiasing().setGlobalValue(graphicsAntialias_.getValue());

      String uiLanguagePrefValue = uiLanguage_.getValue();
      if (!StringUtil.equals(uiLanguagePrefValue, initialUiLanguage_))
      {
         prefs.uiLanguage().setGlobalValue(uiLanguagePrefValue);
         restartRequirement.setUiReloadRequired(true);
      }

      // The uiLanguage preference is mirrored in a cookie telling GWT which language to display.
      // Ensure consistency here and force a page reload if necessary.
      String cookieValue = LocaleCookie.getUiLanguage();
      if (!StringUtil.equals(cookieValue, uiLanguagePrefValue))
      {
         LocaleCookie.setUiLanguage(uiLanguagePrefValue);
         restartRequirement.setUiReloadRequired(true);
      }

      if (BrowseCap.isElectron())
      {
         boolean useNativeDialogsPrefValue = nativeFileDialogs_.getValue();
         if (useNativeDialogsPrefValue != initialNativeFileDialogs_)
         {
            restartRequirement.setUiReloadRequired(true);
         }

         boolean disableRendererAccessibilityPrefValue = disableRendererAccessibility_.getValue();
         if (disableRendererAccessibilityPrefValue != initialDisableRendererAccessibility_)
         {
            if (Desktop.hasDesktopFrame() && BrowseCap.isElectron())
               Desktop.getFrame().setDisableRendererAccessibility(disableRendererAccessibilityPrefValue);
            restartRequirement.setDesktopRestartRequired(true);
         }


         boolean useWebDialogsCookieValue = WebDialogCookie.getUseWebDialogs();
         boolean useWebDialogsPrefValue = !useNativeDialogsPrefValue;
         // The choice of native versus web dialogs is mirrored in a cookie telling GWT which dialogs to use
         // Ensure consistency here and force a page reload if necessary.
         if (useWebDialogsCookieValue != useWebDialogsPrefValue)
         {
            WebDialogCookie.setUseWebDialogs(useWebDialogsPrefValue);
            restartRequirement.setUiReloadRequired(true);
         }
      }

      // Pro specific
      if (showServerHomePage_ != null && showServerHomePage_.isEnabled())
         prefs.showUserHomePage().setGlobalValue(showServerHomePage_.getValue());
      if (reuseSessionsForProjectLinks_ != null && reuseSessionsForProjectLinks_.isEnabled())
         prefs.reuseSessionsForProjectLinks().setGlobalValue(reuseSessionsForProjectLinks_.getValue());
      if (rServerRVersion_ != null && rServerRVersion_.isEnabled())
         prefs.defaultRVersion().setGlobalValue(rServerRVersion_.getRVersion());
      if (rememberRVersionForProjects_ != null && rememberRVersionForProjects_.isEnabled())
         prefs.restoreProjectRVersion().setGlobalValue(rememberRVersionForProjects_.getValue());

      return restartRequirement;
   }

   @Override
   public String getName()
   {
      return constants_.generalTablistLabel();
   }

   @SuppressWarnings("unused")
   private RVersionSpec getDefaultRVersion()
   {
      if (rServerRVersion_ != null)
         return rServerRVersion_.getRVersion();
      else
         return RVersionSpec.createEmpty();
   }

   @SuppressWarnings("unused")
   private boolean getRestoreProjectRVersion()
   {
      if (rememberRVersionForProjects_ != null)
         return rememberRVersionForProjects_.getValue();
      else
         return false;
   }

   private void initializeGraphicsBackendWidget()
   {
      Map<String, String> valuesToLabelsMap = new HashMap<>();
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_DEFAULT, constants_.graphicsBackEndDefaultOption());
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_QUARTZ, constants_.graphicsBackEndQuartzOption());
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_WINDOWS, constants_.graphicsBackEndWindowsOption());
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_CAIRO, constants_.graphicsBackEndCairoOption());
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_CAIRO_PNG, constants_.graphicsBackEndCairoPNGOption());
      valuesToLabelsMap.put(UserPrefs.GRAPHICS_BACKEND_RAGG, constants_.graphicsBackEndAGGOption());

      JsArrayString supportedBackends =
            session_.getSessionInfo().getGraphicsBackends();

      String[] values = new String[supportedBackends.length() + 1];
      values[0] = "default";
      for (int i = 0; i < supportedBackends.length(); i++)
         values[i + 1] = supportedBackends.get(i);

      String[] labels = new String[supportedBackends.length() + 1];
      for (int i = 0; i < labels.length; i++)
         labels[i] = valuesToLabelsMap.get(values[i]);

      graphicsBackend_ =
            new SelectWidget(constants_.graphicsBackendLabel(), labels, values, false, true, false);

      graphicsBackend_.addChangeHandler((ChangeEvent event) ->
      {
         String backend = graphicsBackend_.getValue();
         if (StringUtil.equals(backend, UserPrefs.GRAPHICS_BACKEND_RAGG))
         {
            RStudioGinjector.INSTANCE.getDependencyManager().withRagg(
                  constants_.graphicsBackendUserAction(),
                  (Boolean succeeded) ->
                  {
                     if (!succeeded)
                     {
                        graphicsBackend_.setValue(UserPrefs.GRAPHICS_BACKEND_DEFAULT);
                     }
                  });
         }
      });
   }

   private static final String ENGINE_AUTO        = "auto"; //$NON-NLS-1$
   private static final String ENGINE_DESKTOP     = "desktop"; //$NON-NLS-1$
   private static final String ENGINE_GLES        = "gles"; //$NON-NLS-1$
   private static final String ENGINE_SOFTWARE    = "software"; //$NON-NLS-1$

   private boolean desktopMonitoring_ = false;
   private boolean desktopIgnoreGpuExclusions_ = false;
   private boolean desktopDisableGpuDriverBugWorkarounds_ = false;

   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private RVersionSelectWidget rServerRVersion_ = null;
   private CheckBox rememberRVersionForProjects_ = null;
   private CheckBox reuseSessionsForProjectLinks_ = null;
   private SelectWidget uiLanguage_;
   private CheckBox clipboardMonitoring_ = null;
   private CheckBox fullPathInTitle_ = null;
   private CheckBox nativeFileDialogs_ = null;
   private CheckBox disableRendererAccessibility_ = null;
   private CheckBox useGpuExclusions_ = null;
   private CheckBox useGpuDriverBugWorkarounds_ = null;
   private SelectWidget renderingEngineWidget_ = null;
   private String renderingEngine_ = null;

   private SelectWidget graphicsBackend_;
   private SelectWidget graphicsAntialias_;

   private SelectWidget showServerHomePage_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox removeHistoryDuplicates_;
   private CheckBox restoreLastProject_;
   private CheckBox rProfileOnResume_;
   private CheckBox showLastDotValue_;
   private CheckBox enableCrashReporting_;
   private final UserPrefs prefs_;
   private final Session session_;
   private String initialUiLanguage_;
   private boolean initialNativeFileDialogs_;
   private boolean initialDisableRendererAccessibility_;
}
