/*
 * DesktopApplicationHeader.java
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
package org.rstudio.studio.client.application.ui.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopHooks;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.ApplicationHeader;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderEvent;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderHandler;

public class DesktopApplicationHeader implements ApplicationHeader
{
   public interface Binder
         extends CommandBinder<Commands, DesktopApplicationHeader>
   {
   }
   private static Binder binder_ = GWT.create(Binder.class);

   public DesktopApplicationHeader()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(Commands commands,
                          EventBus events,
                          final Session session,
                          Provider<DesktopHooks> pDesktopHooks)
   {
      session_ = session;
      binder_.bind(commands, this);
      commands.mainMenu(new DesktopMenuCallback());

      pDesktopHooks.get();

      commands.rstudioLicense().setVisible(true);
      commands.rstudioAgreement().setVisible(false);

      commands.uploadFile().remove();
      commands.exportFiles().remove();
      commands.updateCredentials().remove();
      commands.importDatasetFromGoogleSpreadsheet().remove();
      commands.publishPDF().remove();

      commands.checkForUpdates().setVisible(true);
      commands.showLogFiles().setVisible(true);
      commands.showFolder().setVisible(true);

      commands.showAboutDialog().setVisible(true);

      events.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            Scheduler.get().scheduleFinally(new ScheduledCommand()
            {
               public void execute()
               {
                  Desktop.getFrame().onWorkbenchInitialized(
                        session.getSessionInfo().getScratchDir());
               }
            });
         }
      });

      events.addHandler(ShowFolderEvent.TYPE, new ShowFolderHandler()
      {
         public void onShowFolder(ShowFolderEvent event)
         {
            Desktop.getFrame().showFolder(event.getPath().getPath());
         }
      });

   }

   @Handler
   void onUndoDummy()
   {
      Desktop.getFrame().undo();
   }

   @Handler
   void onRedoDummy()
   {
      Desktop.getFrame().redo();
   }

   @Handler
   void onCutDummy()
   {
      Desktop.getFrame().clipboardCut();
   }

   @Handler
   void onCopyDummy()
   {
      Desktop.getFrame().clipboardCopy();
   }

   @Handler
   void onPasteDummy()
   {
      Desktop.getFrame().clipboardPaste();
   }

   @Handler
   void onShowLogFiles()
   {
      Desktop.getFrame().showFolder(session_.getSessionInfo().getLogDir());
   }

   @Handler
   void onCheckForUpdates()
   {
      Desktop.getFrame().checkForUpdates();
   }

   @Handler
   void onShowAboutDialog()
   {
      Desktop.getFrame().showAboutDialog();
   }

   public int getPreferredHeight()
   {
      return 7;
   }

   public Widget toWidget()
   {
      return new HTML();
   }

   private Session session_;
}
