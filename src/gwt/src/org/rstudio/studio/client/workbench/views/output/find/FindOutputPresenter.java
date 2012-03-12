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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.find.FindInFilesDialog.Result;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

public class FindOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>,
                                    HasEnsureHiddenHandlers
   {
      void addMatches(Iterable<FindResult> findResults);
      void clearMatches();
      void ensureVisible(boolean activate);

      HasText getSearchLabel();
      HasClickHandlers getStopSearchButton();
      void setStopSearchButtonVisible(boolean visible);

      void ensureSelectedRowIsVisible();

      HandlerRegistration addSelectionChangedHandler(SelectionChangedHandler handler);
   }

   @Inject
   public FindOutputPresenter(Display view,
                              EventBus events,
                              FindInFilesServerOperations server,
                              final FileTypeRegistry ftr, Session session)
   {
      super(view);
      view_ = view;
      events_ = events;
      server_ = server;
      session_ = session;

      view_.addSelectionChangedHandler(new SelectionChangedHandler()
      {
         @Override
         public void onSelectionChanged(SelectionChangedEvent e)
         {
            view_.ensureSelectedRowIsVisible();
         }
      });

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
      String defaultScopeLabel =
            session_.getSessionInfo().getActiveProjectDir() == null
            ? "(Current working directory)"
            : "(Entire project)";

      new FindInFilesDialog(new OperationWithInput<Result>()
      {
         @Override
         public void execute(final Result input)
         {
            stopAndClear();

            FileSystemItem searchPath =
                  input.getPath() != null
                  ? input.getPath()
                  : session_.getSessionInfo().getActiveProjectDir();

            JsArrayString filePatterns = JsArrayString.createArray().cast();
            for (String pattern : input.getFilePatterns())
               filePatterns.push(pattern);

            server_.beginFind(input.getQuery(),
                              input.isRegex(),
                              !input.isCaseSensitive(),
                              searchPath,
                              filePatterns,
                              new SimpleRequestCallback<String>()
                              {
                                 @Override
                                 public void onResponseReceived(String handle)
                                 {
                                    currentFindHandle_ = handle;
                                    view_.getSearchLabel().setText(
                                          "Find results: " + input.getQuery());
                                    view_.setStopSearchButtonVisible(true);

                                    super.onResponseReceived(handle);

                                    view_.ensureVisible(true);
                                 }
                              });
         }
      }, defaultScopeLabel).showModal();
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
   private final Session session_;
   private EventBus events_;
}
