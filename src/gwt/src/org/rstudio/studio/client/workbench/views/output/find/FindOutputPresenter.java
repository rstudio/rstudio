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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

public class FindOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasSelectionHandlers<CodeNavigationTarget>,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>,
                                    HasEnsureHiddenHandlers
   {
      void addMatches(Iterable<FindResult> findResults);
      void clearMatches();
      void ensureVisible(boolean activate);

      HasText getSearchLabel();
      HasClickHandlers getStopSearchButton();
      void setStopSearchButtonVisible(boolean visible);
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
      events_ = events;
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

      view_.getStopSearchButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            stop();
         }
      });

      events_.addHandler(FindResultEvent.TYPE, new FindResultEvent.Handler()
      {
         @Override
         public void onFindResult(FindResultEvent event)
         {
            if (!event.getHandle().equals(currentFindHandle_))
               return;
            view_.addMatches(event.getResults());
         }
      });

      events_.addHandler(FindOperationEndedEvent.TYPE, new FindOperationEndedEvent.Handler()
      {
         @Override
         public void onFindOperationEnded(
               FindOperationEndedEvent event)
         {
            if (event.getHandle().equals(currentFindHandle_))
            {
               currentFindHandle_ = null;
               view_.setStopSearchButtonVisible(false);
            }
         }
      });
   }

   public void initialize(FindInFilesState state)
   {
      view_.ensureVisible(false);

      currentFindHandle_ = state.getHandle();
      view_.addMatches(state.getResults().toArrayList());
      view_.getSearchLabel().setText("Find results: " + state.getInput());

      if (state.isRunning())
         view_.setStopSearchButtonVisible(true);
      else
         events_.fireEvent(new FindOperationEndedEvent(state.getHandle()));
   }

   @Handler
   public void onFindInFiles()
   {
      globalDisplay_.promptForText("Find", "Find:", "", new OperationWithInput<String>()
      {
         @Override
         public void execute(final String input)
         {
            stopAndClear();

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
                                    view_.getSearchLabel().setText(
                                          "Find results: " + input);
                                    view_.setStopSearchButtonVisible(true);

                                    super.onResponseReceived(handle);

                                    view_.ensureVisible(true);
                                 }
                              });
         }
      });
   }

   public void onDismiss()
   {
      stopAndClear();
      server_.clearFindResults(new VoidServerRequestCallback());
   }

   private void stopAndClear()
   {
      stop();
      view_.clearMatches();
      view_.getSearchLabel().setText("");
   }

   private void stop()
   {
      if (currentFindHandle_ != null)
      {
         server_.stopFind(currentFindHandle_,
                          new VoidServerRequestCallback());
         currentFindHandle_ = null;
      }
      view_.setStopSearchButtonVisible(false);
   }

   private String currentFindHandle_;

   private final Display view_;
   private final FindInFilesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private EventBus events_;
}
