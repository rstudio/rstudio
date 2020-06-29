/*
 * TerminalInfoDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TerminalInfoDialog extends ModalDialogBase
{

   public TerminalInfoDialog(String globalInfo, final TerminalSession session)
   {
      super(Roles.getDialogRole());
      RStudioGinjector.INSTANCE.injectMembers(this);

      setText("Terminal Diagnostics");

      boolean localEchoEnabled = userPrefs_.terminalLocalEcho().getValue() &&
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
         diagnostics.append("Caption:     '").append(cpi.getCaption()).append("'\n");
         diagnostics.append("Title:       '").append(cpi.getTitle()).append("'\n");
         diagnostics.append("Cols x Rows  '").append(cpi.getCols()).append(" x ").append(cpi.getRows()).append("'\n");
         diagnostics.append("Shell:       '").append(TerminalShellInfo.getShellName(cpi.getShellType())).append("'\n");
         diagnostics.append("Handle:      '").append(cpi.getHandle()).append("'\n");
         diagnostics.append("Sequence:    '").append(cpi.getTerminalSequence()).append("'\n");
         diagnostics.append("Restarted:   '").append(cpi.getRestarted()).append("\n");
         if (!BrowseCap.isWindowsDesktop())
            diagnostics.append("Busy:        '").append(cpi.getHasChildProcs()).append("'\n");
         diagnostics.append("Exit Code:   '").append(cpi.getExitCode()).append("'\n");
         diagnostics.append("Full screen: 'client=").append(session.xtermAltBufferActive()).append("/server=").append(cpi.getAltBufferActive()).append("'\n");
         diagnostics.append("Zombie:      '").append(cpi.getZombie()).append("'\n");
         diagnostics.append("Track Env    '").append(cpi.getTrackEnv()).append("'\n");
         diagnostics.append("Local-echo:  '").append(localEchoEnabled).append("'\n");
         diagnostics.append("Working Dir: '").append(cwd).append("'\n");
         diagnostics.append("Interactive: '").append(cpi.getInteractionModeName()).append("'\n");
         diagnostics.append("WebSockets:  '").append(userPrefs_.terminalWebsockets().getValue()).append("'\n");

         diagnostics.append("\nSystem Information\n------------------\n");
         diagnostics.append("Desktop:    '").append(Desktop.isDesktop()).append("'\n");
         diagnostics.append("Remote:     '").append(Desktop.isRemoteDesktop()).append("'\n");
         diagnostics.append("Platform:   '").append(BrowseCap.getPlatformName()).append("'\n");
         if (!Desktop.hasDesktopFrame())
            diagnostics.append("Browser:    '").append(BrowseCap.getBrowserName()).append("'\n");

         diagnostics.append("\nConnection Information\n----------------------\n");
         diagnostics.append(session.getSocket().getConnectionDiagnostics());

         diagnostics.append("\nLocal-echo Match Failures\n-------------------------\n");
         if (!localEchoEnabled)
            diagnostics.append("<Not applicable>\n");
         else
            diagnostics.append(session.getSocket().getLocalEchoDiagnostics());
      }
      textArea_ = new TextArea();
      textArea_.addStyleName(ThemeFonts.getFixedWidthClass());
      textArea_.setSize("600px", "400px");
      textArea_.setReadOnly(true);
      textArea_.setText(diagnostics.toString());

      addOkButton(new ThemedButton("Close", event -> closeDialog()));

      if (session != null)
      {
         appendBufferButton_ = new ThemedButton("Append Buffer", event -> {
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
               }

               @Override
               public void onFailure(String message)
               {
                  diagnostics.append(message);
                  textArea_.setText(diagnostics.toString());
               }
            });
         });
         addLeftButton(appendBufferButton_, ElementIds.PREVIEW_BUTTON);
      }
   }

   @Inject
   private void initialize(UserPrefs uiPrefs)
   {
      userPrefs_ = uiPrefs;
   }

   @Override
   protected Widget createMainWidget()
   {
      return textArea_;
   }

   private TextArea textArea_;
   private ThemedButton appendBufferButton_;

   // Injected ---- 
   private UserPrefs userPrefs_;
}
