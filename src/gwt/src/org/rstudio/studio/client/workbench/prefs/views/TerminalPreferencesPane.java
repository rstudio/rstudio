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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.TerminalPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
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

      add(spaced(new Label("Use the terminal to run system commands, execute data-processing jobs, and more.")));

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
            manageControlVisibility();
         }
      });

      // custom shell path chooser  
      Command onShellPathChosen = new Command()
      {
         @Override
         public void execute()
         {
            if (BrowseCap.isWindowsDesktop())
            {
               String exePath = customShellPathChooser_.getText();
               if (!exePath.endsWith(".exe"))
               {
                  String message = "The program '" + exePath + "'" +
                     " is unlikely to be a valid shell executable.\n" +
                     "Please select an executable ending in '.exe'.";

                  globalDisplay.showMessage(
                        GlobalDisplay.MSG_WARNING,
                        "Invalid Shell Executable",
                        message);
               }
            }
         }
      };

      customShellPathChooser_ = new FileChooserTextBox("",
                                                  "(Default)",
                                                  null,
                                                  onShellPathChosen);
      customShellPathLabel_ = new Label("Custom shell binary:");
      addTextBoxChooser(customShellPathLabel_, null, null, customShellPathChooser_);
      customShellOptionsLabel_ = new Label("Custom shell command-line options:");
      add(spacedBefore(customShellOptionsLabel_));
      customShellOptions_ = new TextBox();
      customShellOptions_.getElement().setAttribute("spellcheck", "false");
      customShellOptions_.setWidth("75%");
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

      HelpLink helpLink = new HelpLink("Using the RStudio terminal", "rstudio_terminal", false);
      nudgeRight(helpLink); 
      helpLink.addStyleName(res_.styles().newSection()); 
      // TODO (gary) -- uncomment once we've published the support article
      //add(helpLink);

      customShellPathChooser_.setEnabled(false);
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

                  for (int i = 0; i < shells.length(); i++)
                  {
                     TerminalShellInfo info = shells.get(i);
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

                  customShellPathChooser_.setText(
                        terminalPrefs.getCustomTerminalShellPath());
                  customShellPathChooser_.setEnabled(true);
                  customShellOptions_.setText(terminalPrefs.getCustomTerminalShellOptions());
                  customShellOptions_.setEnabled(true);
                  manageControlVisibility();
               }

               @Override
               public void onError(ServerError error) { }
            });
         }
      });
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);

      TerminalPrefs terminalPrefs = TerminalPrefs.create(selectedShellType(),
            customShellPathChooser_.getText(),
            customShellOptions_.getText());
      rPrefs.setTerminalPrefs(terminalPrefs);

      return restartRequired;
   }

   private boolean haveLocalEchoPref()
   {
      return !BrowseCap.isWindowsDesktop();
   }

   private boolean haveWebsocketPref()
   {
      return session_.getSessionInfo().getAllowTerminalWebsockets();
   }

   private void addTextBoxChooser(Label captionLabel, HyperlinkLabel link,
         String captionPanelStyle, TextBoxWithButton chooser)
   {
      String textWidth = "250px";

      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      nudgeRight(captionPanel);
      if (captionPanelStyle != null)
         captionPanel.addStyleName(captionPanelStyle);

      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(captionLabel,
            HasHorizontalAlignment.ALIGN_LEFT);

      if (link != null)
      {
         HorizontalPanel linkPanel = new HorizontalPanel();
         linkPanel.add(link);
         captionPanel.add(linkPanel);
         captionPanel.setCellHorizontalAlignment(linkPanel,
               HasHorizontalAlignment.ALIGN_RIGHT);

      }

      add(tight(captionPanel));

      chooser.setTextWidth(textWidth);
      nudgeRight(chooser);
      textBoxWithChooser(chooser);
      add(chooser);
   }

   private int selectedShellType()
   {
      int idx = terminalShell_.getListBox().getSelectedIndex();
      String valStr = terminalShell_.getListBox().getValue(idx);
      return StringUtil.parseInt(valStr, TerminalShellInfo.SHELL_DEFAULT);
   }

   private void manageControlVisibility()
   {
      boolean customEnabled = (selectedShellType() == TerminalShellInfo.SHELL_CUSTOM);
      customShellPathLabel_.setVisible(customEnabled);
      customShellPathChooser_.setVisible(customEnabled);
      customShellOptionsLabel_.setVisible(customEnabled);
      customShellOptions_.setVisible(customEnabled);
   }
 
   private SelectWidget terminalShell_;
   private Label customShellPathLabel_;
   private TextBoxWithButton customShellPathChooser_;
   private Label customShellOptionsLabel_;
   private TextBox customShellOptions_;

   // Injected ----  
   private final UIPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final Session session_;
   private final Server server_;
 }
