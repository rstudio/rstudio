/*
 * DesktopHooks.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import com.google.inject.Inject;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JsObjectInjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedHandler;
import org.rstudio.studio.client.application.events.SuicideEvent;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

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
                       Server server,
                       FileTypeRegistry fileTypeRegistry,
                       WorkbenchContext workbenchContext,
                       SourceShim sourceShim)
   {
      commands_ = commands;
      events_ = events;
      session_ = session;
      globalDisplay_ = globalDisplay;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      workbenchContext_ = workbenchContext;
      sourceShim_ = sourceShim;
      
      events_.addHandler(SaveActionChangedEvent.TYPE, 
                         new SaveActionChangedHandler() 
      {
         public void onSaveActionChanged(SaveActionChangedEvent event)
         {
            saveAction_ = event.getAction();  
         }
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
         return workbenchContext_.getActiveProjectDir().getPath();
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
   
   boolean isSwitchCommand(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null && command.isSwitch();
   }
   
   boolean isSwitchCommandOn(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null && command.isSwitch() && command.isSwitchOn();
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
   
   void quitR()
   {
      commands_.quitSession().execute();
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
   
   String getSumatraPdfExePath()
   {
      return session_.getSessionInfo().getSumatraPdfExePath();
   }

   private final Commands commands_;
   private final EventBus events_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final Server server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final WorkbenchContext workbenchContext_;
   private final SourceShim sourceShim_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
}
