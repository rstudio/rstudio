/*
 * TerminalPreferencesPane.java
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

import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.TerminalPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

public class TerminalPreferencesPane extends PreferencesPane
{

   @Inject
   public TerminalPreferencesPane(UIPrefs prefs,
                                  PreferencesDialogResources res,
                                  Session session,
                                  final GlobalDisplay globalDisplay,
                                  final Server server)
   {
      prefs_ = prefs;
      res_ = res;
      session_ = session;
      server_ = server;

      Label shellLabel = headerLabel("Shell");
      shellLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      add(shellLabel);

      terminalShell_ = new SelectWidget("New terminals open with:");
      spaced(terminalShell_);
      add(terminalShell_);
      terminalShell_.setEnabled(false);
      terminalShell_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            manageCustomShellControlVisibility();
         }
      });

      String textboxWidth = "250px";
      customShellPathLabel_ = new Label("Custom shell binary (fully qualified path):");
      add(spacedBefore(customShellPathLabel_));
      customShellPath_ = new TextBox();
      customShellPath_.getElement().setAttribute("spellcheck", "false");
      customShellPath_.setWidth(textboxWidth);
      add(customShellPath_);
      customShellPath_.setEnabled(false);

      customShellOptionsLabel_ = new Label("Custom shell command-line options:");
      add(spacedBefore(customShellOptionsLabel_));
      customShellOptions_ = new TextBox();
      customShellOptions_.getElement().setAttribute("spellcheck", "false");
      customShellOptions_.setWidth(textboxWidth);
      customShellOptions_.setEnabled(false);
      add(customShellOptions_);

      Label perfLabel = headerLabel("Connection");
      perfLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      add(perfLabel);
 
      boolean showPerfLabel = false;
      if (haveLocalEchoPref())
      {
         CheckBox chkTerminalLocalEcho = checkboxPref("Local terminal echo",
               prefs_.terminalLocalEcho(), 
               "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.");
         add(chkTerminalLocalEcho);
         showPerfLabel = true;
      }
      if (haveWebsocketPref())
      {
         CheckBox chkTerminalWebsocket = checkboxPref("Connect with WebSockets",
               prefs_.terminalUseWebsockets(), 
               "WebSockets are generally more responsive; try turning off if terminal won't connect.");
         add(chkTerminalWebsocket);
         showPerfLabel = true;
      }

      perfLabel.setVisible(showPerfLabel);

      Label miscLabel = headerLabel("Miscellaneous");
      miscLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      add(miscLabel);
      miscLabel.setVisible(true);

      CheckBox chkTerminalAutoClose = checkboxPref("Close terminal when shell exits",
            prefs_.terminalAutoClose(),
            "Deselect this option to keep terminal pane open after shell exits.");
      add(chkTerminalAutoClose);

      if (haveCaptureEnvPref())
      {
         CheckBox chkCaptureEnv = checkboxPref("Save and restore environment variables",
               prefs_.terminalTrackEnvironment(),
               "Terminal occasionally runs a hidden command to capture state of environment variables.");
         add(chkCaptureEnv);
      }

      if (haveBusyDetectionPref())
      {
         Label shutdownLabel = headerLabel("Process Termination");
         shutdownLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
         add(shutdownLabel);
         shutdownLabel.setVisible(true);

         busyMode_ = new SelectWidget("Ask before killing processes:");
         spaced(busyMode_);
         add(busyMode_);
         busyMode_.setEnabled(false);
         busyMode_.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event)
            {
               manageBusyModeControlVisibility();
            }
         });
         busyWhitelistLabel_ = new Label("Don't ask before killing:");
         add(busyWhitelistLabel_);
         busyWhitelist_ = new TextBox();
         busyWhitelist_.getElement().setAttribute("spellcheck", "false");
         busyWhitelist_.setWidth(textboxWidth);
         add(busyWhitelist_);
         busyWhitelist_.setEnabled(false);
      }
      
      HelpLink helpLink = new HelpLink("Using the RStudio terminal", "rstudio_terminal", false);
      nudgeRight(helpLink); 
      helpLink.addStyleName(res_.styles().newSection()); 
      // TODO (gary) -- uncomment once we've published the support article
      //add(helpLink);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconTerminal2x());
   }

   @Override
   public String getName()
   {
      return "Terminal";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
      final TerminalPrefs terminalPrefs = prefs.getTerminalPrefs();

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            server_.getTerminalShells(new ServerRequestCallback<JsArray<TerminalShellInfo>>()
            {
               @Override
               public void onResponseReceived(JsArray<TerminalShellInfo> shells)
               {
                  int currentShell = terminalPrefs.getDefaultTerminalShellValue();
                  int currentShellIndex = 0;

                  TerminalPreferencesPane.this.terminalShell_.getListBox().clear();

                  boolean hasCustom = false;

                  for (int i = 0; i < shells.length(); i++)
                  {
                     TerminalShellInfo info = shells.get(i);
                     if (info.getShellType() == TerminalShellInfo.SHELL_CUSTOM)
                        hasCustom = true;
                     TerminalPreferencesPane.this.terminalShell_.addChoice(
                           info.getShellName(), Integer.toString(info.getShellType()));
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
                     customShellPath_.setText(
                           terminalPrefs.getCustomTerminalShellPath());
                     customShellPath_.setEnabled(true);
                     customShellOptions_.setText(terminalPrefs.getCustomTerminalShellOptions());
                     customShellOptions_.setEnabled(true);
                  }
                  manageCustomShellControlVisibility();
               }

               @Override
               public void onError(ServerError error) { }
            });
         }
      });

      if (busyMode_ != null)
      {
         busyMode_.getListBox().clear();
         busyMode_.addChoice("Always", Integer.toString(UIPrefsAccessor.BUSY_DETECT_ALWAYS));
         busyMode_.addChoice("Never", Integer.toString(UIPrefsAccessor.BUSY_DETECT_NEVER));
         busyMode_.addChoice("Always except for whitelist", Integer.toString(UIPrefsAccessor.BUSY_DETECT_WHITELIST));
         busyMode_.setEnabled(true);
         
         int selection = prefs_.terminalBusyMode().getValue();
         if (selection < UIPrefsAccessor.BUSY_DETECT_ALWAYS ||
               selection > UIPrefsAccessor.BUSY_DETECT_WHITELIST)
            selection = UIPrefsAccessor.BUSY_DETECT_ALWAYS;
         
         busyMode_.getListBox().setSelectedIndex(selection);
         
         List<String> whitelistArray = JsArrayUtil.fromJsArrayString(
               prefs_.terminalBusyWhitelist().getValue());
         
         StringBuilder whitelist = new StringBuilder();
         for (String entry: whitelistArray)
         {
            if (entry.trim().isEmpty())
            {
               continue;
            }
            if (whitelist.length() > 0)
            {
               whitelist.append(" ");
            }
            whitelist.append(entry.trim());
         }

         busyWhitelist_.setText(whitelist.toString());
         busyWhitelist_.setEnabled(true);

         manageBusyModeControlVisibility();
      }
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);
     
      if (haveBusyDetectionPref())
      {
         prefs_.terminalBusyWhitelist().setGlobalValue(StringUtil.split(busyWhitelist_.getText(), " "));
         prefs_.terminalBusyMode().setGlobalValue(selectedBusyMode());
      } 
      TerminalPrefs terminalPrefs = TerminalPrefs.create(selectedShellType(),
            customShellPath_.getText(),
            customShellOptions_.getText());
      rPrefs.setTerminalPrefs(terminalPrefs);

      return restartRequired;
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

   private int selectedShellType()
   {
      int idx = terminalShell_.getListBox().getSelectedIndex();
      String valStr = terminalShell_.getListBox().getValue(idx);
      return StringUtil.parseInt(valStr, TerminalShellInfo.SHELL_DEFAULT);
   }

   private void manageCustomShellControlVisibility()
   {
      boolean customEnabled = (selectedShellType() == TerminalShellInfo.SHELL_CUSTOM);
      customShellPathLabel_.setVisible(customEnabled);
      customShellPath_.setVisible(customEnabled);
      customShellOptionsLabel_.setVisible(customEnabled);
      customShellOptions_.setVisible(customEnabled);
   }

   private int selectedBusyMode()
   {
      int idx = busyMode_.getListBox().getSelectedIndex();
      String valStr = busyMode_.getListBox().getValue(idx);
      return StringUtil.parseInt(valStr, UIPrefsAccessor.BUSY_DETECT_ALWAYS);
   }

   private void manageBusyModeControlVisibility()
   {
      boolean whitelistEnabled = selectedBusyMode() == UIPrefsAccessor.BUSY_DETECT_WHITELIST;
      busyWhitelistLabel_.setVisible(whitelistEnabled);
      busyWhitelist_.setVisible(whitelistEnabled);
   }
  
   private SelectWidget terminalShell_;
   private Label customShellPathLabel_;
   private TextBox customShellPath_;
   private Label customShellOptionsLabel_;
   private TextBox customShellOptions_;

   private SelectWidget busyMode_;
   private Label busyWhitelistLabel_;
   private TextBox busyWhitelist_;
   
   // Injected ----  
   private final UIPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final Session session_;
   private final Server server_;
 }
