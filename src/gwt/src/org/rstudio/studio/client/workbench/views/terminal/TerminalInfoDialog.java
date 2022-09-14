/*
 * TerminalInfoDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

      setText(constants_.terminalDiagnosticsText());

      boolean localEchoEnabled = userPrefs_.terminalLocalEcho().getValue() &&
            !BrowseCap.isWindowsDesktop();

      final StringBuilder diagnostics = new StringBuilder();

      diagnostics.append(constants_.globalTerminalInformationText());
      diagnostics.append(globalInfo);
      if (session != null)
      {
         ConsoleProcessInfo cpi = session.getProcInfo();

         String cwd = cpi.getCwd();
         if (StringUtil.isNullOrEmpty(cwd))
            cwd = "Default";

         diagnostics.append("\n" + constants_.globalTerminalInformationText());
         diagnostics.append(constants_.captionText()).append(cpi.getCaption()).append("'\n");
         diagnostics.append(constants_.titleText()).append(cpi.getTitle()).append("'\n");
         diagnostics.append(constants_.colsText()).append(cpi.getCols()).append(" x ").append(cpi.getRows()).append("'\n");
         diagnostics.append(constants_.shellText()).append(TerminalShellInfo.getShellName(cpi.getShellType())).append("'\n");
         diagnostics.append(constants_.handleText()).append(cpi.getHandle()).append("'\n");
         diagnostics.append(constants_.sequenceText()).append(cpi.getTerminalSequence()).append("'\n");
         diagnostics.append(constants_.restartedText()).append(cpi.getRestarted()).append("\n");
         if (!BrowseCap.isWindowsDesktop())
            diagnostics.append(constants_.busyText()).append(cpi.getHasChildProcs()).append("'\n");
         diagnostics.append(constants_.exitCodeText()).append(cpi.getExitCode()).append("'\n");
         diagnostics.append(constants_.fullScreenText()).append(session.xtermAltBufferActive()).append("/server=").append(cpi.getAltBufferActive()).append("'\n");
         diagnostics.append(constants_.zombieText()).append(cpi.getZombie()).append("'\n");
         diagnostics.append(constants_.trackEnvText()).append(cpi.getTrackEnv()).append("'\n");
         diagnostics.append(constants_.localEchoText()).append(localEchoEnabled).append("'\n");
         diagnostics.append(constants_.workingDirText()).append(cwd).append("'\n");
         diagnostics.append(constants_.interactiveText()).append(cpi.getInteractionModeName()).append("'\n");
         diagnostics.append(constants_.webSocketsText()).append(userPrefs_.terminalWebsockets().getValue()).append("'\n");

         diagnostics.append(constants_.systemInformationText());
         diagnostics.append(constants_.desktopText()).append(Desktop.isDesktop()).append("'\n");
         diagnostics.append(constants_.remoteText()).append(Desktop.isRemoteDesktop()).append("'\n");
         diagnostics.append(constants_.platformText()).append(BrowseCap.getPlatformName()).append("'\n");
         if (!Desktop.hasDesktopFrame())
            diagnostics.append(constants_.browserText()).append(BrowseCap.getBrowserName()).append("'\n");

         diagnostics.append(constants_.connectionInformationText());
         diagnostics.append(session.getSocket().getConnectionDiagnostics());

         diagnostics.append(constants_.matchFailuresText());
         if (!localEchoEnabled)
            diagnostics.append(constants_.notApplicableText());
         else
            diagnostics.append(session.getSocket().getLocalEchoDiagnostics());
      }
      textArea_ = new TextArea();
      textArea_.addStyleName(ThemeFonts.getFixedWidthClass());
      textArea_.setSize("600px", "400px");
      textArea_.setReadOnly(true);
      textArea_.setText(diagnostics.toString());

      addOkButton(new ThemedButton(constants_.closeTitle(), event -> closeDialog()));

      if (session != null)
      {
         appendBufferButton_ = new ThemedButton(constants_.appendBufferTitle(), event -> {
            appendBufferButton_.setEnabled(false);
            diagnostics.append(constants_.terminalBufferText());
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
   private static final TerminalConstants constants_ = com.google.gwt.core.client.GWT.create(TerminalConstants.class);
}
