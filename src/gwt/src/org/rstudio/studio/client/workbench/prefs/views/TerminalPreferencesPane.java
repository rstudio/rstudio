/*
 * TerminalPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

public class TerminalPreferencesPane extends PreferencesPane
{

   @Inject
   public TerminalPreferencesPane(UserPrefs prefs,
                                  PreferencesDialogResources res,
                                  Session session,
                                  final GlobalDisplay globalDisplay,
                                  final Server server)
   {
      prefs_ = prefs;
      res_ = res;
      session_ = session;
      server_ = server;

      VerticalTabPanel general = new VerticalTabPanel(ElementIds.TERMINAL_GENERAL_PREFS);
      VerticalTabPanel closing = new VerticalTabPanel(ElementIds.TERMINAL_CLOSING_PREFS);

      Label shellLabel = headerLabel(constants_.shellHeaderLabel());
      shellLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      general.add(shellLabel);

      initialDirectory_ = new SelectWidget(
            constants_.initialDirectoryLabel(),
            new String[]
                  {
                        constants_.projectDirectoryOption(),
                        constants_.currentDirectoryOption(),
                        constants_.homeDirectoryOption()
                  },
            new String[]
                  {
                        UserPrefs.TERMINAL_INITIAL_DIRECTORY_PROJECT,
                        UserPrefs.TERMINAL_INITIAL_DIRECTORY_CURRENT,
                        UserPrefs.TERMINAL_INITIAL_DIRECTORY_HOME
                  },
            false, true, false);
      spaced(initialDirectory_);
      general.add(initialDirectory_);

      terminalShell_ = new SelectWidget(constants_.terminalShellLabel());
      spaced(terminalShell_);
      general.add(terminalShell_);
      terminalShell_.setEnabled(false);
      terminalShell_.addChangeHandler(event -> manageCustomShellControlVisibility());
      terminalShell_.addChangeHandler(event -> managePythonIntegrationControlVisibility());

      // custom shell exe path chooser
      Command onShellExePathChosen = new Command()
      {
         @Override
         public void execute()
         {
            managePythonIntegrationControlVisibility();
            
            if (BrowseCap.isWindowsDesktop())
            {
               String shellExePath = customShellChooser_.getText();
               if (!shellExePath.endsWith(".exe"))
               {
                  String message = constants_.shellExePathMessage(shellExePath);

                  globalDisplay.showMessage(
                        GlobalDisplay.MSG_WARNING,
                        constants_.shellExeCaption(),
                        message);
               }
            }
         }
      };

      String textboxWidth = "250px";
      customShellPathLabel_ = new FormLabel(constants_.customShellPathLabel());
      customShellChooser_ = new FileChooserTextBox(customShellPathLabel_,
                                                   constants_.customShellChooserEmptyLabel(),
                                                   ElementIds.TextBoxButtonId.TERMINAL,
                                                   false,
                                                   null,
                                                   onShellExePathChosen);
      addTextBoxChooser(general, textboxWidth, customShellPathLabel_, customShellChooser_);
      customShellChooser_.setEnabled(false);

      customShellOptions_ = new TextBox();
      DomUtils.disableSpellcheck(customShellOptions_);
      customShellOptions_.setWidth(textboxWidth);
      customShellOptions_.setEnabled(false);
      customShellOptionsLabel_ = new FormLabel(constants_.customShellOptionsLabel(), customShellOptions_);
      general.add(spacedBefore(customShellOptionsLabel_));
      general.add(spaced(customShellOptions_));
      
      
      chkPythonIntegration_ = checkboxPref(
            constants_.chkPythonIntegration(),
            prefs_.terminalPythonIntegration());
      
      chkPythonIntegration_.setTitle(
            constants_.chkPythonIntegrationTitle());

      general.add(chkPythonIntegration_);

      Label perfLabel = headerLabel(constants_.perfLabel());
      perfLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      general.add(perfLabel);

      boolean showPerfLabel = false;
      if (haveLocalEchoPref())
      {
         CheckBox chkTerminalLocalEcho = checkboxPref(constants_.chkTerminalLocalEchoLabel(),
               prefs_.terminalLocalEcho(),
               constants_.chkTerminalLocalEchoTitle());
         general.add(chkTerminalLocalEcho);
         showPerfLabel = true;
      }
      if (haveWebsocketPref())
      {
         CheckBox chkTerminalWebsocket = checkboxPref(constants_.chkTerminalWebsocketLabel(),
               prefs_.terminalWebsockets(),
               constants_.chkTerminalWebsocketTitle());
         general.add(chkTerminalWebsocket);
         showPerfLabel = true;
      }

      perfLabel.setVisible(showPerfLabel);

      Label displayLabel = headerLabel(constants_.displayHeaderLabel());
      displayLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      general.add(displayLabel);
      chkHardwareAcceleration_ = new CheckBox(constants_.chkHardwareAccelerationLabel());
      general.add(lessSpaced(chkHardwareAcceleration_));
      chkAudibleBell_ = new CheckBox(constants_.chkAudibleBellLabel());
      general.add(lessSpaced(chkAudibleBell_));
      chkWebLinks_ = new CheckBox(constants_.chkWebLinksLabel());
      general.add(chkWebLinks_);

      HelpLink helpLink = new HelpLink(constants_.helpRStudioAccessibilityLinkLabel(), "rstudio_terminal", false);
      nudgeRight(helpLink);
      helpLink.addStyleName(res_.styles().newSection());
      general.add(helpLink);

      Label miscLabel = headerLabel(constants_.miscLabel());
      miscLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      closing.add(miscLabel);
      miscLabel.setVisible(true);

      autoClosePref_ = new SelectWidget(
            constants_.autoClosePrefLabel(),
            new String[]
                  {
                        constants_.closePaneOption(),
                        constants_.doNotClosePaneOption(),
                        constants_.shellExitsPaneOption()
                  },
            new String[]
                  {
                        UserPrefs.TERMINAL_CLOSE_BEHAVIOR_ALWAYS,
                        UserPrefs.TERMINAL_CLOSE_BEHAVIOR_NEVER,
                        UserPrefs.TERMINAL_CLOSE_BEHAVIOR_CLEAN
                  },
            false, true, false);
      spaced(autoClosePref_);
      closing.add(autoClosePref_);

      if (haveCaptureEnvPref())
      {
         CheckBox chkCaptureEnv = checkboxPref(constants_.chkCaptureEnvLabel(),
               prefs_.terminalTrackEnvironment(),
                 constants_.chkCaptureEnvTitle());
         closing.add(chkCaptureEnv);
      }

      if (haveBusyDetectionPref())
      {
         Label shutdownLabel = headerLabel(constants_.shutdownLabel());
         shutdownLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
         closing.add(shutdownLabel);
         shutdownLabel.setVisible(true);

         busyMode_ = new SelectWidget(constants_.busyModeLabel());
         spaced(busyMode_);
         closing.add(busyMode_);
         busyMode_.setEnabled(false);
         busyMode_.addChangeHandler(event -> manageBusyModeControlVisibility());
         busyExclusionList_ = new TextBox();
         DomUtils.disableSpellcheck(busyExclusionList_);
         busyExclusionList_.setWidth(textboxWidth);
         busyExclusionListLabel_ = new FormLabel(constants_.busyWhitelistLabel(), busyExclusionList_);
         closing.add(busyExclusionListLabel_);
         closing.add(busyExclusionList_);
         busyExclusionList_.setEnabled(false);
      }

      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel(constants_.terminalPaneLabel());
      tabPanel.setSize("435px", "533px");
      tabPanel.add(general, constants_.tabGeneralPanelLabel(), general.getBasePanelId());
      tabPanel.add(closing, constants_.tabClosingPanelLabel(), closing.getBasePanelId());
      tabPanel.selectTab(0);
      add(tabPanel);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconTerminal2x());
   }

   @Override
   public String getName()
   {
      return constants_.terminalPaneLabel();
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      Scheduler.get().scheduleDeferred(() -> server_.getTerminalShells(
            new ServerRequestCallback<JsArray<TerminalShellInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<TerminalShellInfo> shells)
         {
            String currentShell = BrowseCap.isWindowsDesktop() ?
               prefs.windowsTerminalShell().getValue() :
               prefs.posixTerminalShell().getValue();
            int currentShellIndex = 0;

            TerminalPreferencesPane.this.terminalShell_.getListBox().clear();

            boolean hasCustom = false;

            for (int i = 0; i < shells.length(); i++)
            {
               TerminalShellInfo info = shells.get(i);
               if (StringUtil.equals(info.getShellType(), UserPrefs.WINDOWS_TERMINAL_SHELL_CUSTOM))
                  hasCustom = true;
               TerminalPreferencesPane.this.terminalShell_.addChoice(
                     info.getShellName(), info.getShellType());
               if (info.getShellType() == currentShell)
                  currentShellIndex = i;
            }
            if (TerminalPreferencesPane.this.terminalShell_.getListBox().getItemCount() > 0)
            {
               TerminalPreferencesPane.this.terminalShell_.setEnabled((true));
               TerminalPreferencesPane.this.terminalShell_.getListBox().setSelectedIndex(currentShellIndex);
            }

            if (hasCustom)
            {
               customShellChooser_.setText(prefs.customShellCommand().getValue());
               customShellChooser_.setEnabled(true);
               customShellOptions_.setText(prefs.customShellOptions().getValue());
               customShellOptions_.setEnabled(true);
            }
            manageCustomShellControlVisibility();
         }

         @Override
         public void onError(ServerError error) { }
      }));

      if (busyMode_ != null)
      {
         busyMode_.getListBox().clear();
         busyMode_.addChoice(constants_.busyModeAlwaysOption(), UserPrefs.BUSY_DETECTION_ALWAYS);
         busyMode_.addChoice(constants_.busyModeNeverOption(), UserPrefs.BUSY_DETECTION_NEVER);
         busyMode_.addChoice(constants_.busyModeListOption(), UserPrefs.BUSY_DETECTION_LIST);
         busyMode_.setEnabled(true);

         prefs_.busyDetection().getValue();
         for (int i = 0; i < busyMode_.getListBox().getItemCount(); i++)
         {
            if (busyMode_.getListBox().getValue(i) == prefs_.busyDetection().getValue())
            {
               busyMode_.getListBox().setSelectedIndex(i);
            }
         }

         List<String> exclusionArray = JsArrayUtil.fromJsArrayString(
               prefs_.busyExclusionList().getValue());

         StringBuilder exclusionList = new StringBuilder();
         for (String entry: exclusionArray)
         {
            if (entry.trim().isEmpty())
            {
               continue;
            }
            if (exclusionList.length() > 0)
            {
               exclusionList.append(" ");
            }
            exclusionList.append(entry.trim());
         }

         busyExclusionList_.setText(exclusionList.toString());
         busyExclusionList_.setEnabled(true);

         manageBusyModeControlVisibility();
      }

      chkAudibleBell_.setValue(prefs_.terminalBellStyle().getValue() == UserPrefsAccessor.TERMINAL_BELL_STYLE_SOUND);
      chkWebLinks_.setValue(prefs_.terminalWeblinks().getValue());
      chkHardwareAcceleration_.setValue(prefs_.terminalRenderer().getValue() == UserPrefsAccessor.TERMINAL_RENDERER_CANVAS);

      if (!initialDirectory_.setValue(prefs.terminalInitialDirectory().getValue()))
         initialDirectory_.getListBox().setSelectedIndex(0);

      if (!autoClosePref_.setValue(prefs.terminalCloseBehavior().getValue()))
         autoClosePref_.getListBox().setSelectedIndex(0);
   }

   @Override
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      RestartRequirement restartRequirement = super.onApply(rPrefs);

      if (haveBusyDetectionPref())
      {
         prefs_.busyExclusionList().setGlobalValue(StringUtil.split(busyExclusionList_.getText(), " "));
         prefs_.busyDetection().setGlobalValue(selectedBusyMode());
      }

      if (BrowseCap.isWindowsDesktop())
         prefs_.windowsTerminalShell().setGlobalValue(selectedShellType());
      else
         prefs_.posixTerminalShell().setGlobalValue(selectedShellType());

      prefs_.customShellCommand().setGlobalValue(customShellChooser_.getText());
      prefs_.customShellOptions().setGlobalValue(customShellOptions_.getText());

      prefs_.terminalBellStyle().setGlobalValue(chkAudibleBell_.getValue() ?
            UserPrefsAccessor.TERMINAL_BELL_STYLE_SOUND : UserPrefsAccessor.TERMINAL_BELL_STYLE_NONE);
      prefs_.terminalRenderer().setGlobalValue(chkHardwareAcceleration_.getValue() ?
            UserPrefsAccessor.TERMINAL_RENDERER_CANVAS : UserPrefsAccessor.TERMINAL_RENDERER_DOM);
      prefs_.terminalWeblinks().setGlobalValue(chkWebLinks_.getValue());

      prefs_.terminalInitialDirectory().setGlobalValue(initialDirectory_.getValue());
      prefs_.terminalCloseBehavior().setGlobalValue(autoClosePref_.getValue());

      return restartRequirement;
   }

   private boolean haveLocalEchoPref()
   {
      return !BrowseCap.isWindowsDesktop();
   }

   private boolean haveBusyDetectionPref()
   {
      return !BrowseCap.isWindowsDesktop();
   }

   private boolean haveWebsocketPref()
   {
      return session_.getSessionInfo().getAllowTerminalWebsockets();
   }

   private boolean haveCaptureEnvPref()
   {
      return !BrowseCap.isWindowsDesktop();
   }

   private String selectedShellType()
   {
      return terminalShell_.getListBox().getSelectedValue();
   }

   private void manageCustomShellControlVisibility()
   {
      boolean customEnabled = (selectedShellType() == UserPrefs.WINDOWS_TERMINAL_SHELL_CUSTOM);
      customShellPathLabel_.setVisible(customEnabled);
      customShellChooser_.setVisible(customEnabled);
      customShellOptionsLabel_.setVisible(customEnabled);
      customShellOptions_.setVisible(customEnabled);
   }
   
   private boolean pythonIntegrationSupported()
   {
      String shell = terminalShell_.getValue();
      if (StringUtil.equals(shell, "bash") ||
          StringUtil.equals(shell, "zsh"))
      {
         return true;
      }
      
      if (StringUtil.equals(shell, "custom"))
      {
         String shellPath = customShellChooser_.getText();
         if (shellPath.endsWith("bash") ||
             shellPath.endsWith("zsh") ||
             shellPath.endsWith("bash.exe") ||
             shellPath.endsWith("zsh.exe"))
         {
            return true;
         }
      }
      
      return false;
   }
   
   private void managePythonIntegrationControlVisibility()
   {
      if (pythonIntegrationSupported())
      {
         chkPythonIntegration_.setEnabled(true);
         chkPythonIntegration_.setVisible(true);
      }
      else
      {
         chkPythonIntegration_.setEnabled(false);
         chkPythonIntegration_.setVisible(false);
      }
   }

   private String selectedBusyMode()
   {
      int idx = busyMode_.getListBox().getSelectedIndex();
      return busyMode_.getListBox().getValue(idx);
   }

   private void manageBusyModeControlVisibility()
   {
      boolean exclusionListEnabled = selectedBusyMode() == UserPrefs.BUSY_DETECTION_LIST;
      busyExclusionListLabel_.setVisible(exclusionListEnabled);
      busyExclusionList_.setVisible(exclusionListEnabled);
   }

   private void addTextBoxChooser(Panel panel, String textWidth, FormLabel captionLabel, TextBoxWithButton chooser)
   {
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      nudgeRight(captionPanel);

      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(captionLabel, HasHorizontalAlignment.ALIGN_LEFT);

      panel.add(tight(captionPanel));

      chooser.setTextWidth(textWidth);
      nudgeRight(chooser);
      textBoxWithChooser(chooser);
      spaced(chooser);
      panel.add(chooser);
   }

   private final SelectWidget terminalShell_;
   private final FormLabel customShellPathLabel_;
   private final TextBoxWithButton customShellChooser_;
   private final FormLabel customShellOptionsLabel_;
   private final TextBox customShellOptions_;
   private final SelectWidget initialDirectory_;

   private final CheckBox chkHardwareAcceleration_;
   private final CheckBox chkAudibleBell_;
   private final CheckBox chkWebLinks_;
   private final CheckBox chkPythonIntegration_;

   private SelectWidget autoClosePref_;
   private SelectWidget busyMode_;
   private FormLabel busyExclusionListLabel_;
   private TextBox busyExclusionList_;

   // Injected ----
   private final UserPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final Session session_;
   private final Server server_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);}
