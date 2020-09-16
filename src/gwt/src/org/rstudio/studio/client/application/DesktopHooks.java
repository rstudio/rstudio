/*
 * DesktopHooks.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JsObjectInjector;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.LauncherServerEvent;
import org.rstudio.studio.client.application.events.MouseNavigateSourceHistoryEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SuicideEvent;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.ui.impl.DesktopApplicationHeader;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

/**
 * Any methods on this class are automatically made available to the
 * Qt frame code.
 */
public class DesktopHooks
{
   @BaseExpression("$wnd.desktopHooks")
   interface DesktopHooksInjector extends JsObjectInjector<DesktopHooks> {}
   private static final DesktopHooksInjector injector =
         GWT.create(DesktopHooksInjector.class);

   @Inject
   public DesktopHooks(Commands commands,
                       EventBus events,
                       Session session,
                       GlobalDisplay globalDisplay,
                       Provider<UserPrefs> pUIPrefs,
                       Server server,
                       FileTypeRegistry fileTypeRegistry,
                       WorkbenchContext workbenchContext,
                       ProductEditionInfo editionInfo)
   {
      commands_ = commands;
      events_ = events;
      session_ = session;
      globalDisplay_ = globalDisplay;
      pUIPrefs_ = pUIPrefs;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      workbenchContext_ = workbenchContext;
      editionInfo_ = editionInfo;
      
      events_.addHandler(SaveActionChangedEvent.TYPE, saveActionChangedEvent ->
      {
         saveAction_ = saveActionChangedEvent.getAction();
      });
      
      injector.injectObject(this);

      addCopyHook();
   }

   private native void addCopyHook() /*-{
      var clean = function() {
         setTimeout(function() {
            $wnd.desktop.cleanClipboard(false);
         }, 100)
      };
      $wnd.addEventListener("copy", clean, true);
      $wnd.addEventListener("cut", clean, true);
   }-*/;

   
   String getActiveProjectDir()
   {
      if (workbenchContext_.getActiveProjectDir() != null)
      {
         if (pUIPrefs_.get().fullProjectPathInWindowTitle().getValue())
            return workbenchContext_.getActiveProjectDir().getPath();
         else
            return workbenchContext_.getActiveProjectDir().getName();
      }
      else
         return "";
   }
   
   void invokeCommand(String cmdId)
   {
      commands_.getCommandById(cmdId).execute();
   }

   boolean isCommandVisible(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null && command.isVisible();
   }

   boolean isCommandEnabled(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null && command.isEnabled();
   }
   
   boolean isCommandChecked(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null && command.isChecked();
   }
   
   String getCommandLabel(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null ? command.getMenuLabel(true) : "";
   }

   void openFile(String filePath)
   {
      // get the file system item
      FileSystemItem file = FileSystemItem.createFile(filePath);
      
      if (file.isDirectory())
         return;
      
      // this used to be possible but shouldn't be anymore
      // (since we screen out .rproj from calling sendMessage
      // within DesktopMain.cpp
      if (file.getExtension().equalsIgnoreCase(".rproj"))
         return;
      
      // open the file. pass false for second param to prevent
      // the default handler (the browser) from taking it
      fileTypeRegistry_.openFile(file, false);
   }
   
   void evaluateRCmd(final String cmd)
   {
      // inject a 100ms delay between execution of commands to prevent
      // issues with commands being delivered out of order by cocoa
      // networking to the server (it appears as if when you put e.g. 10
      // requests in flight simultaneously it's not guaranteed that they
      // will be received in the order they were sent).
      commandQueue_.addCommand(new SerializedCommand() {
         @Override
         public void onExecute(final Command continuation)
         {
            // execute the code
            events_.fireEvent(new SendToConsoleEvent(cmd, true));
            
            // wait 100ms to execute the next command in the queue
            new Timer() {

               @Override
               public void run()
               {
                  continuation.execute();  
               }
            }.schedule(100);
         }
      });
      
      // make sure the queue is running
      commandQueue_.run();  
   }
   
   void quitR()
   {
      commands_.quitSession().execute();
   }

   void promptToQuitR()
   {
      globalDisplay_.showYesNoMessage(MessageDialog.QUESTION,
            "Close Remote Session",
            "Do you want to close the remote session?",
            false,
            (Operation) () -> commands_.quitSession().execute(),
            (Operation) () -> Desktop.getFrame().onSessionQuit(),
            true);
   }
  
   void notifyRCrashed()
   {
      events_.fireEvent(new SuicideEvent(""));
   }
   
   int getSaveAction()
   {
      return saveAction_.getAction();
   }
   
   String getREnvironmentPath()
   {
      return workbenchContext_.getREnvironmentPath();
   }
   
   boolean isSelectionEmpty()
   {
      return DesktopApplicationHeader.isSelectionEmpty();
   }
   
   void licenseLost(String licenseMessage)
   {
      String message = "Unable to obtain a license. Please restart RStudio to try again.";
      if (!StringUtil.isNullOrEmpty(licenseMessage))
      {
         message = message + "\n\nDetails: ";
         message = message + licenseMessage;
      }
      globalDisplay_.showMessage(MessageDialog.WARNING, editionInfo_.editionName(), message,
            (Operation) () -> commands_.forceQuitSession().execute());
   }
   
   void updateLicenseWarningBar(String licenseMessage)
   {
      if (StringUtil.isNullOrEmpty(licenseMessage))
         globalDisplay_.hideWarningBar();
      else
         globalDisplay_.showLicenseWarningBar(false, licenseMessage);
   }

   void onLauncherServerEvent(String eventType, String details)
   {
      LauncherServerEvent.EventType type = LauncherServerEvent.EventType.valueOf(eventType);
      events_.fireEvent(new LauncherServerEvent(type, details));
   }

   void mouseNavigateButtonClick(boolean forward, int x, int y)
   {
      events_.fireEvent(new MouseNavigateSourceHistoryEvent(forward, x, y));
   }
   
   void onDragStart()
   {
      workbenchContext_.setDroppedUrls(null);
   }
   
   void onUrlsDropped(String droppedUrls)
   {   
      final String kUrlSeparator = "26D63FFA-995F-4E9A-B4AA-04DA9F93B538";
      List<String> urls = Arrays.asList(droppedUrls.split(kUrlSeparator));
      workbenchContext_.setDroppedUrls(urls); 
   }

   private final Commands commands_;
   private final EventBus events_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final Provider<UserPrefs> pUIPrefs_;
   private final Server server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final WorkbenchContext workbenchContext_;
   private final SerializedCommandQueue commandQueue_ = 
                                         new SerializedCommandQueue();
   private final ProductEditionInfo editionInfo_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
}
