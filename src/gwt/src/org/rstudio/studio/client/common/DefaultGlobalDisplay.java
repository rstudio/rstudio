/*
 * DefaultGlobalDisplay.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
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

      // This command is useful for testing warning bars (e.g. for accessibility) so please leave in.
      commands.showWarningBar().addHandler(appCommand ->
      {
         view_.get().showWarning(false, "This is a warning!");
      });
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             final OperationWithInput<String> operation)
   {
      promptForText(title, label, initialValue, false, operation);
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             boolean optional,
                             final OperationWithInput<String> operation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title, label, initialValue, optional ? MessageDisplay.INPUT_OPTIONAL_TEXT : MessageDisplay.INPUT_REQUIRED_TEXT,
            -1, -1, null,
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
                             int type,
                             OperationWithInput<String> operation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title, label, "", type, -1, -1, null,
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
            title, label, initialValue, 
            MessageDisplay.INPUT_REQUIRED_TEXT,
            -1, -1, null, operation, null);
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
            MessageDisplay.INPUT_REQUIRED_TEXT,
            selectionOffset,
            selectionLength,
            okButtonCaption,
            operation,
            null);
   }

   @Override
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             int selectionOffset,
                             int selectionLength,
                             String okButtonCaption,
                             ProgressOperationWithInput<String> operation,
                             Operation cancelOperation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForText(
            title,
            label,
            initialValue,
            MessageDisplay.INPUT_REQUIRED_TEXT,
            selectionOffset,
            selectionLength,
            okButtonCaption,
            operation,
            cancelOperation);
   }

   @Override
   public void promptForTextWithOption(
                                 String title,
                                 String label,
                                 String initialValue,
                                 int type,
                                 String extraOptionPrompt,
                                 boolean extraOptionDefault,
                                 ProgressOperationWithInput<PromptWithOptionResult> okOperation,
                                 Operation cancelOperation)
   {
      ((TextInput)GWT.create(TextInput.class)).promptForTextWithOption(
            title,
            label,
            initialValue,
            type,
            extraOptionPrompt,
            extraOptionDefault,
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
            MessageDisplay.INPUT_NUMERIC,
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

   public void showLicenseWarningBar(boolean severe, String message)
   {
      view_.get().showLicenseWarning(severe, message);
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
            onProgress(message, null);
         }
         
         public void onProgress(String message, Operation onCancel)
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
      NewWindowOptions options = new NewWindowOptions();
      options.setName("_blank");
      options.setFocus(true);
      openMinimalWindow(url, showLocation, width, height, options);
   }
   
   @Override
   public void openMinimalWindow(String url,
                                 boolean showLocation,
                                 int width, 
                                 int height, 
                                 NewWindowOptions options)
   {
      windowOpener_.openMinimalWindow(this,
                                      url,
                                      options,
                                      width,
                                      height,
                                      showLocation);
   }
   
   @Override
   public void openWebMinimalWindow(String url,
                                    boolean showLocation,
                                    int width, 
                                    int height, 
                                    NewWindowOptions options)
   {
      windowOpener_.openWebMinimalWindow(this,
                                         url,
                                         options,
                                         width,
                                         height,
                                         showLocation);
   }

   @Override
   public void openSatelliteWindow(String name, int width, int height)
   {
      openSatelliteWindow(name, width, height, null);
   }
   
   @Override
   public void openSatelliteWindow(String name, int width, int height, 
         NewWindowOptions options)
   {
      windowOpener_.openSatelliteWindow(this, name, width, height, options);
   }

   @Override
   public void bringWindowToFront(String name)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().activateMinimalWindow(name);
      else
         bringWindowToFrontImpl(name);
   }
   
   private static final native void bringWindowToFrontImpl(String name)
   /*-{
      $wnd.open("", name);
   }-*/;
   
   @Override
   public void openRStudioLink(String linkName, boolean includeVersionInfo)
   {
      // build url
      final SessionInfo sessionInfo = session_.getSessionInfo();
      String url = "https://www.rstudio.org/links/";
      url += URL.encodePathSegment(linkName);
      if (includeVersionInfo)
      {
         url += "?version=" + URL.encodeQueryString(sessionInfo.getRstudioVersion());
         url += "&mode=" + URL.encodeQueryString(sessionInfo.getMode());
      }
      
      // open window
      openWindow(url);
   }
   
   @Override
   public void showHtmlFile(String path)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().showFile(StringUtil.notNull(path));
      else if (Desktop.isRemoteDesktop())
         Desktop.getFrame().browseUrl(server_.getFileUrl(FileSystemItem.createFile(path)));
      else
         openWindow(server_.getFileUrl(FileSystemItem.createFile(path)));
   }
   
   @Override
   public void showWordDoc(String path)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().showWordDoc(StringUtil.notNull(path));
      else
         openWindow(server_.getFileUrl(FileSystemItem.createFile(path)));
   }

   @Override
   public void showPptPresentation(String path)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().showPptPresentation(StringUtil.notNull(path));
      else
         openWindow(server_.getFileUrl(FileSystemItem.createFile(path)));
   }

   private final Provider<ApplicationView> view_;
   private final Session session_;
   private final ApplicationServerOperations server_;
   private final WindowOpener windowOpener_ = GWT.create(WindowOpener.class);
}

