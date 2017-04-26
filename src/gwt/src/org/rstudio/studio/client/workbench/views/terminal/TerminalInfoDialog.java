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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TerminalInfoDialog extends ModalDialogBase
{

   public TerminalInfoDialog(TerminalSession session, TerminalSessionSocket socket)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      setText("Terminal Diagnostics - " + session.getCaption());

      boolean localEchoEnabled = uiPrefs_.terminalLocalEcho().getValue() && 
            !BrowseCap.isWindowsDesktop();
      
      StringBuilder diagnostics = new StringBuilder();
      diagnostics.append("Terminal Session Information\n----------------------------\n");
      diagnostics.append("Caption:    '" + session.getCaption() + "'\n");
      diagnostics.append("Title:      '" + session.getTitle() + "'\n");
      diagnostics.append("Shell:      '" + TerminalShellInfo.getShellName(session.getShellType()) + "'\n");
      diagnostics.append("Handle:     '" + session.getHandle() + "'\n");
      diagnostics.append("Sequence:   '" + session.getSequence() + "'\n");
      diagnostics.append("Busy:       '" + session.getHasChildProcs() + "'\n");
      diagnostics.append("Alt-Buffer: '" + session.altBufferActive() + "'\n");
      diagnostics.append("Local-echo: '" + localEchoEnabled + "'\n"); 
      diagnostics.append("WebSockets: '" + uiPrefs_.terminalUseWebsockets().getValue() + "'\n");
      diagnostics.append("Report lag: '" + uiPrefs_.enableReportTerminalLag().getValue() + "'\n");

      diagnostics.append("\nSystem Information\n------------------\n");
      diagnostics.append("Desktop:    '" + Desktop.isDesktop() + "'\n");
      diagnostics.append("Platform:   '" + BrowseCap.getPlatformName() + "'\n");
      diagnostics.append("Browser:    '" + BrowseCap.getBrowserName() + "'\n");

      diagnostics.append("\nConnection Information\n----------------------\n");
      diagnostics.append(socket.getConnectionDiagnostics());
      
      diagnostics.append("\nLocal-echo Match Failures\n-------------------------\n");
      if (!localEchoEnabled)
         diagnostics.append("<Not applicable>\n");
      else
         diagnostics.append(socket.getLocalEchoDiagnostics());
     
      textArea_ = new TextArea();
      textArea_.addStyleName(ThemeResources.INSTANCE.themeStyles().fixedWidthFont());
      textArea_.setSize("600px", "400px");
      textArea_.setReadOnly(true);
      textArea_.setText(diagnostics.toString());

      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      ThemedButton closeButton = new ThemedButton("Close",
                                                  new ClickHandler() {
         public void onClick(ClickEvent event) {
            closeDialog();
         }
      });
      addOkButton(closeButton); 
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

   // Injected ---- 
   private UIPrefs uiPrefs_;
}
