/*
 * TerminalInfoDialog.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TerminalInfoDialog extends ModalDialogBase
{

   public TerminalInfoDialog(String globalInfo, final TerminalSession session)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      setText("Terminal Diagnostics");

      boolean localEchoEnabled = uiPrefs_.terminalLocalEcho().getValue() && 
            !BrowseCap.isWindowsDesktop();
      
      final StringBuilder diagnostics = new StringBuilder();
      
      diagnostics.append("Global Terminal Information\n---------------------------\n");
      diagnostics.append(globalInfo);
      if (session != null)
      {
         ConsoleProcessInfo cpi = session.getProcInfo();

         String cwd = cpi.getCwd();
         if (StringUtil.isNullOrEmpty(cwd))
            cwd = "Default";

         diagnostics.append("\nCurrent Terminal Session Information\n------------------------------------\n");
         diagnostics.append("Caption:     '" + cpi.getCaption() + "'\n");
         diagnostics.append("Title:       '" + cpi.getTitle() + "'\n");
         diagnostics.append("Cols x Rows  '" + cpi.getCols() + " x " + cpi.getRows() + "'\n");
         diagnostics.append("Shell:       '" + TerminalShellInfo.getShellName(cpi.getShellType()) + "'\n");
         diagnostics.append("Handle:      '" + cpi.getHandle() + "'\n");
         diagnostics.append("Sequence:    '" + cpi.getTerminalSequence() + "'\n");
         diagnostics.append("Restarted:   '" + cpi.getRestarted() + "\n");
         diagnostics.append("Busy:        '" + cpi.getHasChildProcs() + "'\n");
         diagnostics.append("Exit Code:   '" + cpi.getExitCode() + "'\n");
         diagnostics.append("Full screen: 'client=" + session.xtermAltBufferActive() +  
               "/server=" + cpi.getAltBufferActive() + "'\n"); 
         diagnostics.append("Zombie:      '" + cpi.getZombie() + "'\n");
         diagnostics.append("Track Env    '" + cpi.getTrackEnv() + "'\n");
         diagnostics.append("Local-echo:  '" + localEchoEnabled + "'\n"); 
         diagnostics.append("Working Dir: '" + cwd + "'\n"); 
         diagnostics.append("Interactive: '" + cpi.getInteractionModeName() + "'\n");
         diagnostics.append("WebSockets:  '" + uiPrefs_.terminalUseWebsockets().getValue() + "'\n");
         diagnostics.append("Typing lag:  '" + session.getSocket().getTypingLagMsg() + "'\n");

         diagnostics.append("\nSystem Information\n------------------\n");
         diagnostics.append("Desktop:    '" + Desktop.isDesktop() + "'\n");
         diagnostics.append("Platform:   '" + BrowseCap.getPlatformName() + "'\n");
         if (!Desktop.isDesktop())
            diagnostics.append("Browser:    '" + BrowseCap.getBrowserName() + "'\n");

         diagnostics.append("\nConnection Information\n----------------------\n");
         diagnostics.append(session.getSocket().getConnectionDiagnostics());

         diagnostics.append("\nLocal-echo Match Failures\n-------------------------\n");
         if (!localEchoEnabled)
            diagnostics.append("<Not applicable>\n");
         else
            diagnostics.append(session.getSocket().getLocalEchoDiagnostics());
      }
      textArea_ = new TextArea();
      textArea_.addStyleName(ThemeResources.INSTANCE.themeStyles().fixedWidthFont());
      textArea_.setSize("600px", "400px");
      textArea_.setReadOnly(true);
      textArea_.setText(diagnostics.toString());

      addOkButton(new ThemedButton("Close", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event) {
            closeDialog();
         }
      }));
      
      if (session != null)
      {
         appendBufferButton_ = new ThemedButton("Append Buffer", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
               appendBufferButton_.setEnabled(false);
               diagnostics.append("\n\nTerminal Buffer (Server)\n---------------\n");
               session.getBuffer(false /*stripAnsiCodes*/, new ResultCallback<String, String>()
               {
                  @Override
                  public void onSuccess(String buffer)
                  {
                     diagnostics.append(AnsiCode.prettyPrint(buffer));
                     textArea_.setText(diagnostics.toString());
                     textArea_.setCursorPos(diagnostics.toString().length());
                     textArea_.getElement().setScrollTop(textArea_.getElement().getScrollHeight());

                     diagnostics.append("\n\nTerminal Buffer (Client)\n---------------\n");
                     diagnostics.append(AnsiCode.prettyPrint(session.getLocalBuffer()));
                     textArea_.setText(diagnostics.toString());
                  }

                  @Override
                  public void onFailure(String message)
                  {
                     diagnostics.append(message);
                     textArea_.setText(diagnostics.toString());
                  }
               });
            }
         });
         addLeftButton(appendBufferButton_);
      }      
   }

   @Inject
   private void initialize(UIPrefs uiPrefs)
   {
      uiPrefs_ = uiPrefs;
   } 

   
   @Override
   protected Widget createMainWidget()
   {
      return textArea_;
   }
   
   TextArea textArea_;
   ThemedButton appendBufferButton_;

   // Injected ---- 
   private UIPrefs uiPrefs_;
}
