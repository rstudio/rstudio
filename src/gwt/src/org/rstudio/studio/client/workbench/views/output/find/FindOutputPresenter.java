/*
 * FindOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.events.FindInFilesResultEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

public class FindOutputPresenter extends BasePresenter
   implements FindInFilesResultEvent.Handler
{
   public interface Display extends WorkbenchView,
                                    HasSelectionHandlers<CodeNavigationTarget>,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>
   {
      void addMatches(Iterable<FindResult> findResults);
      void clearMatches();
      void ensureVisible();

      int getFileCount();
      void setFileOpen(int index, boolean open);
   }

   @Inject
   public FindOutputPresenter(Display view,
                              EventBus events,
                              FindInFilesServerOperations server,
                              GlobalDisplay globalDisplay,
                              final FileTypeRegistry ftr, Session session)
   {
      super(view);
      view_ = view;
      server_ = server;
      globalDisplay_ = globalDisplay;
      session_ = session;

      view_.addSelectionCommitHandler(new SelectionCommitHandler<CodeNavigationTarget>()
      {
         @Override
         public void onSelectionCommit(SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            if (target == null)
               return;

            ftr.editFile(FileSystemItem.createFile(target.getFile()),
                         target.getPosition());
         }
      });

      events.addHandler(FindResultEvent.TYPE, new FindResultEvent.Handler()
      {
         @Override
         public void onFindResult(FindResultEvent event)
         {
            if (!event.getHandle().equals(currentFindHandle_))
               return;

            final int count = view_.getFileCount();
            view_.addMatches(event.getResults());
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  for (int i = count; i < view_.getFileCount(); i++)
                  {
                     view_.setFileOpen(i, true);
                  }
               }
            });
         }
      });
   }

   @Override
   public void onFindInFilesResult(FindInFilesResultEvent event)
   {
      view_.bringToFront();
   }

   @Handler
   public void onFindInFiles()
   {
      globalDisplay_.promptForText("Find", "Find:", "", new OperationWithInput<String>()
      {
         @Override
         public void execute(String input)
         {
            // TODO: Show indication that search is in progress
            // TODO: Provide way to cancel a running search

            if (currentFindHandle_ != null)
            {
               server_.stopFind(currentFindHandle_,
                                new VoidServerRequestCallback());
               currentFindHandle_ = null;
               view_.clearMatches();
            }

            server_.beginFind(input,
                              false,
                              true,
                              session_.getSessionInfo().getActiveProjectDir(),
                              "",
                              new SimpleRequestCallback<String>()
                              {
                                 @Override
                                 public void onResponseReceived(String handle)
                                 {
                                    currentFindHandle_ = handle;

                                    super.onResponseReceived(handle);
                                    // TODO: add tab to view using handle ID

                                    view_.ensureVisible();
                                 }
                              });
         }
      });
   }

   private String currentFindHandle_;

   private final Display view_;
   private final FindInFilesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
}
