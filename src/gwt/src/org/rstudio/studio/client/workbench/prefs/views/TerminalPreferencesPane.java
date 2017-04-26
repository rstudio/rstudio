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
import org.rstudio.core.client.widget.SelectWidget;
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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class TerminalPreferencesPane extends PreferencesPane
{

   @Inject
   public TerminalPreferencesPane(UIPrefs prefs,
                                  PreferencesDialogResources res,
                                  Session session,
                                  final Server server)
   {
      prefs_ = prefs;
      res_ = res;
      session_ = session;
      server_ = server;

      add(spaced(new Label("Use the terminal to run system commands, execute data-processing jobs, and more.")));
      
      if (haveTerminalShellPref())
      {
         terminalShell_ = new SelectWidget("Default terminal shell:");
         spaced(terminalShell_);
         add(terminalShell_);
         terminalShell_.setEnabled(false);
      }
      if (haveLocalEchoPref())
      {
         CheckBox chkTerminalLocalEcho = checkboxPref("Local terminal echo",
               prefs_.terminalLocalEcho(), 
               "Local echo is more responsive but may get out of sync with some line-editing modes.");
         add(chkTerminalLocalEcho);
      }
      if (haveWebsocketPref())
      {
         CheckBox chkTerminalWebsocket = checkboxPref("Connect with WebSockets",
               prefs_.terminalUseWebsockets(), 
               "WebSockets are generally more responsive; try turning off if terminal won't connect.");
         add(chkTerminalWebsocket);
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

      if (terminalShell_ != null)
      {
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
                  }

                  @Override
                  public void onError(ServerError error) { }
               });
            }
         });
      }
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);
      
      int defaultShell = TerminalShellInfo.SHELL_DEFAULT;
      if (terminalShell_ != null && terminalShell_.isEnabled())
      {
         int idx = terminalShell_.getListBox().getSelectedIndex();
         String valStr = terminalShell_.getListBox().getValue(idx);
         defaultShell = StringUtil.parseInt(valStr, TerminalShellInfo.SHELL_DEFAULT);
      }

      TerminalPrefs terminalPrefs = TerminalPrefs.create(defaultShell);
      rPrefs.setTerminalPrefs(terminalPrefs);

      return restartRequired;
   }

   private boolean haveTerminalShellPref()
   {
      return BrowseCap.isWindowsDesktop();
   }

   private boolean haveLocalEchoPref()
   {
      return !BrowseCap.isWindowsDesktop();
   }
   
   private boolean haveWebsocketPref()
   {
      return session_.getSessionInfo().getAllowTerminalWebsockets();
   }
 
   private SelectWidget terminalShell_;

   // Injected ----  
   private final UIPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final Session session_;
   private final Server server_;
 }
