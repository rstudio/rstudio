/*
 * RSConnectConnectAccountDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;

public class RSConnectConnectAccountDialog 
       extends RSConnectDialog<RSConnectConnectAccount>
{
   public RSConnectConnectAccountDialog(RSConnectServerOperations server, 
                                        final GlobalDisplay display)
   {
      super(server, display, new RSConnectConnectAccount());
      display_ = display;
      server_ = server;

      setText("Connect Account");
      setWidth("450px");
      HTML createLink = new HTML("<small>Need an account?<br />" +
            "Get started at <a href=\"http://shinyapps.io/\"" + 
            "target=\"blank\">http://shinyapps.io</a></small>");
      createLink.setStyleName(contents_.getStyle().spaced());
      addLeftWidget(createLink);
      connectButton_ = new ThemedButton("Connect");
      connectButton_.setEnabled(false);
      connectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onConnect();
         }
      });
      addCancelButton();
      addOkButton(connectButton_);
      contents_.setOnAccountInfoChanged(new Command()
      {
         @Override
         public void execute()
         {
            connectButton_.setEnabled(contents_.getAccountInfo().length() > 0);
         }
      });
   }

   private void onConnect()
   {
      // get command and substitute rsconnect for shinyapps
      final String cmd = contents_.getAccountInfo().replace("shinyapps::", 
                                                            "rsconnect::");
      
      if (!cmd.startsWith("rsconnect::setAccountInfo"))
      {
         display_.showErrorMessage("Error Connecting Account", 
               "The pasted command should start with " + 
               "rsconnect::setAccountInfo. If you're having trouble, try " + 
               "connecting your account manually; type " +
               "?rsconnect::setAccountInfo at the R console for help.");
         return;
      }
      server_.connectRSConnectAccount(cmd, 
            new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            closeDialog();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account",  
                  "The command '" + cmd + "' failed. You can set up an " + 
                  "account manually by using rsconnect::setAccountInfo; " +
                  "type ?rsconnect::setAccountInfo at the R console for " +
                  "more information.");
         }
      });
   }
   
   private ThemedButton connectButton_;
}