/*
 * DefaultGlobalDisplay.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.studio.client.common.dialog.DialogBuilderFactory;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class DefaultGlobalDisplay extends GlobalDisplay
{
   @Inject
   public DefaultGlobalDisplay(Provider<ApplicationView> view,
                               Commands commands,
                               Session session,
                               ApplicationServerOperations server)
   {
      view_ = view;
      session_ = session;
      server_ = server;

      commands.showWarningBar().addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            view_.get().showWarning(false, "This is a warning!");
         }
      });
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             final OperationWithInput<String> operation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title, label, initialValue, false, false, -1, -1, null,
            new ProgressOperationWithInput<String>()
            {
               public void execute(String input, ProgressIndicator indicator)
               {
                  indicator.onCompleted();
                  operation.execute(input);
               }
            },
            null);
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             ProgressOperationWithInput<String> operation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title, label, initialValue, false, false, -1, -1, null, operation, null);
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             int selectionOffset,
                             int selectionLength,
                             String okButtonCaption,
                             ProgressOperationWithInput<String> operation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title,
            label,
            initialValue,
            false,
            false,
            selectionOffset,
            selectionLength,
            okButtonCaption,
            operation,
            null);
   }

   @Override
   public void promptForPassword(String title,
                                 String label,
                                 String initialValue,
                                 String rememberPasswordPrompt,
                                 boolean rememberByDefault,
                                 ProgressOperationWithInput<PasswordResult> okOperation,
                                 Operation cancelOperation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForPassword(
            title,
            label,
            initialValue,
            rememberPasswordPrompt,
            rememberByDefault,
            -1,
            -1,
            null,
            okOperation,
            cancelOperation);
   }

   @Override
   public void promptForInteger(String title,
                                String label,
                                Integer initialValue,
                                final ProgressOperationWithInput<Integer> okOperation,
                                Operation cancelOperation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title,
            label,
            initialValue == null ? "" : initialValue.toString(),
            false,
            true,
            -1,
            -1,
            null,
            new ProgressOperationWithInput<String>()
            {
               @Override
               public void execute(String input, ProgressIndicator indicator)
               {  
                  int value = Integer.parseInt(input.trim());
                  okOperation.execute(value, indicator);
               }
            },
            cancelOperation);
   }

   @Override
   protected DialogBuilder createDialog(int type,
                                        String caption,
                                        String message)
   {
      return ((DialogBuilderFactory)GWT.create(DialogBuilderFactory.class))
            .create(type, caption, message);
   }

   public Command showProgress(String message)
   {
      return SlideLabel.show(message, false, true, RootLayoutPanel.get());
   }

   public void showWarningBar(boolean severe, String message)
   {
      view_.get().showWarning(severe, message);
   }

   public void hideWarningBar()
   {
      view_.get().hideWarning();
   }

   public ProgressIndicator getProgressIndicator(final String errorCaption)
   {
      return new ProgressIndicator()
      {
         public void onProgress(String message)
         {
            dismissProgress();
            dismissProgress_ = showProgress(message);
         }
         
         public void clearProgress()
         {
            dismissProgress();
         }

         private void dismissProgress()
         {
            if (dismissProgress_ != null)
               dismissProgress_.execute();
            dismissProgress_ = null;
         }

         public void onCompleted()
         {
            dismissProgress();
         }

         public void onError(String message)
         {
            dismissProgress();
            showMessage(GlobalDisplay.MSG_ERROR, errorCaption, message);
         }

         private Command dismissProgress_;
      };
   }
   
   @Override
   public void openWindow(String url)
   {
      openWindow(url, null);
   }

   @Override
   public void openWindow(String url, NewWindowOptions options)
   {
      if (options == null)
         options = new NewWindowOptions();
      
      windowOpener_.openWindow(this,
                               url,
                               options);
   }
   
   @Override
   public void openProgressWindow(String name,
                                  String message,
                                  OperationWithInput<WindowEx> openOperation)
   {
      String url = server_.getApplicationURL("progress");
      url += "?message=" + URL.encodeQueryString(message);
      NewWindowOptions options = new NewWindowOptions();
      options.setName(name);
      options.setCallback(openOperation);
      openWindow(url, options);
   }
   
   @Override
   public void openMinimalWindow(String url, int width, int height)
   {
      openMinimalWindow(url, false, width, height);
   }
   
   @Override
   public void openMinimalWindow(String url,
                                 boolean showLocation,
                                 int width, 
                                 int height)
   {
      openMinimalWindow(url, showLocation, width, height, "_blank", true);
   }
   
   @Override
   public void openMinimalWindow(String url,
                                 boolean showLocation,
                                 int width, 
                                 int height, 
                                 String name,
                                 boolean focus)
   {
      NewWindowOptions options = new NewWindowOptions();
      options.setName(name);
      options.setFocus(focus);
      windowOpener_.openMinimalWindow(this,
                                      url,
                                      options,
                                      width,
                                      height,
                                      showLocation);
   }
   
   @Override
   public void openSatelliteWindow(String name, int width, int height)
   {
      windowOpener_.openSatelliteWindow(this, name, width, height);
   }
   

   @Override
   public void openEmailComposeWindow(String to, String subject)
   {
      // determine gmail url
      String gmailURL = "https://mail.google.com/";
      String user = session_.getSessionInfo().getUserIdentity();  
      if (user == null) // for desktop mode
         user = "foo@gmail.com"; 
      String[] userComponents = user.split("@");
      if ( (userComponents.length == 2) &&
           ("gmail.com").equalsIgnoreCase(userComponents[1]))
      {
         gmailURL += "mail/";
      }
      else
      {
         gmailURL += "a/" + userComponents[1] + "/";
      }
      
      // calculate URL
      String url = gmailURL + "?fs=1&view=cm";
      url += "&to=" + URL.encodeQueryString(to);
      if (subject != null)
         url += "&subject=" + URL.encodeQueryString(subject);
      
      // open window
      openWindow(url);
   }
   
   @Override
   public void openRStudioLink(String linkName, boolean includeVersionInfo)
   {
      // build url
      final SessionInfo sessionInfo = session_.getSessionInfo();
      String url = "http://www.rstudio.org/links/" ;
      url += URL.encodePathSegment(linkName) ;
      if (includeVersionInfo)
      {
         url += "?version=" + URL.encodeQueryString(sessionInfo.getRstudioVersion());
         url += "&mode=" + URL.encodeQueryString(sessionInfo.getMode());
      }
      
      // open window
      openWindow(url);
   }

   private final Provider<ApplicationView> view_;
   private final Session session_;
   private final ApplicationServerOperations server_;
   private final WindowOpener windowOpener_ = GWT.create(WindowOpener.class);
}

