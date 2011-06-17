/*
 * DesktopHooks.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.events.BarrierReleasedEvent;
import org.rstudio.core.client.events.BarrierReleasedHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JsObjectInjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedHandler;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
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

   /**
    * Delay showing progress until DELAY_MILLIS have elapsed.
    */
   private class ProgressDelayer
   {
      private ProgressDelayer(final String progressMessage)
      {
         final int DELAY_MILLIS = 250;

         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               dismiss_ = globalDisplay_.showProgress(progressMessage);
            }
         };
         timer_.schedule(DELAY_MILLIS);
      }

      public void dismiss()
      {
         timer_.cancel();
         if (dismiss_ != null)
            dismiss_.execute();
      }

      private final Timer timer_;
      private Command dismiss_;
   }

   @Inject
   public DesktopHooks(Commands commands,
                       EventBus events,
                       GlobalDisplay globalDisplay,
                       Server server,
                       FileTypeRegistry fileTypeRegistry,
                       WorkbenchContext workbenchContext,
                       SourceShim sourceShim)
   {
      commands_ = commands;
      events_ = events;
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


   void saveChangesBeforeQuit()
   {
      sourceShim_.saveChangesBeforeQuit(new Command() {
         public void execute()
         {
            Desktop.getFrame().closeWithSaveConfirmed();
         }
      });
   }

   void quitR(final boolean saveChanges)
   {
      final ProgressDelayer progress = new ProgressDelayer(
            "Quitting R Session...");

      // Use a barrier and LastChanceSaveEvent to allow source documents
      // and client state to be synchronized before quitting.

      Barrier barrier = new Barrier();
      barrier.addBarrierReleasedHandler(new BarrierReleasedHandler()
      {
         public void onBarrierReleased(BarrierReleasedEvent event)
         {
            // All last chance save operations have completed (or possibly
            // failed). Now do the real quit.

            server_.quitSession(
                  saveChanges,
                  new VoidServerRequestCallback(
                        globalDisplay_.getProgressIndicator("Error Quitting R")) {

                     @Override
                     public void onResponseReceived(org.rstudio.studio.client.server.Void response)
                     {
                        progress.dismiss();
                        super.onResponseReceived(response);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        progress.dismiss();
                        super.onError(error);
                     }
                  });
         }
      });

      // We acquire a token to make sure that the barrier doesn't fire before
      // all the LastChanceSaveEvent listeners get a chance to acquire their
      // own tokens.
      Token token = barrier.acquire();
      try
      {
         events_.fireEvent(new LastChanceSaveEvent(barrier));
      }
      finally
      {
         token.release();
      }
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

   String getCommandLabel(String commandId)
   {
      AppCommand command = commands_.getCommandById(commandId);
      return command != null ? command.getMenuLabel(true) : "";
   }

   void openFile(String filePath)
   {
      // get the file system item
      FileSystemItem file = FileSystemItem.createFile(filePath);
      
      // don't open directories (these can sneak in if the user 
      // passes a directory on the command line)
      if (!file.isDirectory())
      {
         // open the file. pass false for second param to prevent
         // the default handler (the browser) from taking it
         fileTypeRegistry_.openFile(file, false);
      }
   }
   
   int getSaveAction()
   {
      return saveAction_.getAction();
   }
   
   String getREnvironmentPath()
   {
      return workbenchContext_.getREnvironmentPath();
   }

   private final Commands commands_;
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Server server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final WorkbenchContext workbenchContext_;
   private final SourceShim sourceShim_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
}
