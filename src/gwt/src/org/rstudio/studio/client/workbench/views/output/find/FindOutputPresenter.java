/*
 * FindOutputPresenter.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.find.events.FindInFilesEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.PreviewReplaceEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceProgressEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

import java.util.ArrayList;

public class FindOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>,
                                    HasEnsureHiddenHandlers
   {
      void addMatches(ArrayList<FindResult> findResults);
      void clearMatches();
      void ensureVisible(boolean activate);

      HasClickHandlers getStopSearchButton();
      void setStopSearchButtonVisible(boolean visible);

      void ensureSelectedRowIsVisible();

      HandlerRegistration addSelectionChangedHandler(SelectionChangedHandler handler);

      void showOverflow();
      
      void showSearchCompleted();

      void updateSearchLabel(String query, String path);
      void updateSearchLabel(String query, String path, String replace);
      void clearSearchLabel();

      boolean getReplaceMode();
      public void toggleReplaceMode();
      HasClickHandlers getReplaceAllButton();
      String getReplaceText();
      boolean isReplaceRegex();
      boolean useGitIgnore();

      HasClickHandlers getStopReplaceButton();
      void setStopReplaceButtonVisible(boolean visible);
      void enableReplace();
      void disableReplace();

      void showProgress();
      void hideProgress();
      ProgressBar getProgress();
   }

   @Inject
   public FindOutputPresenter(Display view,
                              EventBus events,
                              FindInFilesServerOperations server,
                              final FileTypeRegistry ftr,
                              Session session,
                              WorkbenchContext workbenchContext)
   {
      super(view);
      view_ = view;
      events_ = events;
      server_ = server;
      session_ = session;
      workbenchContext_ = workbenchContext;

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
            if (event.getHandle() != currentFindHandle_)
               return;

            //view_.clearMatches();
            view_.addMatches(event.getResults());

            view_.ensureVisible(true);

            {
               int count = 0;
               for (FindResult fr : event.getResults())
                  count += fr.getMatchOns().size();
               dialogState_.updateResultsCount(count);
            }
            // replace may have been previously disabled
            view_.enableReplace();
            Debug.logToConsole("Find Result Event with " + dialogState_.getResultsCount() + " results.");
         }
      });

      events_.addHandler(FindOperationEndedEvent.TYPE, new FindOperationEndedEvent.Handler()
      {
         @Override
         public void onFindOperationEnded(
               FindOperationEndedEvent event)
         {
            if (event.getHandle() == currentFindHandle_)
            {
               currentFindHandle_ = null;
               view_.setStopSearchButtonVisible(false);
               view_.showSearchCompleted();
            }
         }
      });

      events_.addHandler(PreviewReplaceEvent.TYPE, new PreviewReplaceEvent.Handler()
      {
         @Override
         public void onPreviewReplace(PreviewReplaceEvent event)
         {
            stopAndClear();

            FileSystemItem searchPath =
                                      FileSystemItem.createDir(dialogState_.getPath());
            JsArrayString filePatterns = JsArrayString.createArray().cast();
            for (String pattern : dialogState_.getFilePatterns())
               filePatterns.push(pattern);

            server_.previewReplace(dialogState_.getQuery(),
                                   dialogState_.isRegex(),
                                   !dialogState_.isCaseSensitive(),
                                   searchPath,
                                   filePatterns,
                                   view_.getReplaceText(),
                                   view_.isReplaceRegex(),
                                   view_.useGitIgnore(),
                                   new SimpleRequestCallback<String>()
                                   {
                                      @Override
                                      public void onResponseReceived(String handle)
                                      {
                                         currentFindHandle_ = handle;
                                      }
                                   });
         }
      });

      view_.getStopReplaceButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Stop Replace",
                  "Are you sure you want to cancel the replace? Changes already made will not be reverted.",
                  new Operation ()
                  {
                     @Override
                     public void execute()
                     {
                        stopReplace();
                     }
                  },
                  false);
         }
      });

      view_.getReplaceAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String message = "Are you sure you wish to permanently replace all? This will replace " +
                             dialogState_.getResultsCount();
            if (dialogState_.isRegex() ||
                view_.isReplaceRegex())
               message += " occurences and cannot be undone.";
            else
               message += " occurences of '" + dialogState_.getQuery() +
                          "' with '" + view_.getReplaceText() +
                          "' and cannot be undone.";
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Replace All",
                  message,
                  new Operation ()
                  {
                     @Override
                     public void execute()
                     {
                        view_.setStopReplaceButtonVisible(true);
                        stopAndClear();

                        FileSystemItem searchPath =
                                                  FileSystemItem.createDir(dialogState_.getPath());
                        JsArrayString filePatterns = JsArrayString.createArray().cast();
                        for (String pattern : dialogState_.getFilePatterns())
                           filePatterns.push(pattern);

                        server_.completeReplace(dialogState_.getQuery(),
                                                dialogState_.isRegex(),
                                                !dialogState_.isCaseSensitive(),
                                                searchPath,
                                                filePatterns,
                                                dialogState_.getResultsCount(),
                                                view_.getReplaceText(),
                                                view_.isReplaceRegex(),
                                                view_.useGitIgnore(),
                                                new SimpleRequestCallback<String>()
                                                {
                                                   @Override
                                                   public void onResponseReceived(String handle)
                                                   {
                                                      currentFindHandle_ = handle;
                                                      updateSearchLabel(dialogState_.getQuery(),
                                                                        dialogState_.getPath(),
                                                                        dialogState_.isRegex(),
                                                                        view_.getReplaceText(),
                                                                        view_.isReplaceRegex());
                                                   }
                                                });
                     }
                  },
                  false);
        }
      });

      events_.addHandler(ReplaceProgressEvent.TYPE, new ReplaceProgressEvent.Handler()
      {
         @Override
         public void onReplaceProgress(ReplaceProgressEvent event)
         {
            Debug.logToConsole("Replace progress event " + event.units() + " units out of " + event.max());
            view_.showProgress();
            view_.getProgress().setProgress(event.units(), event.max());
            if (event.units() == event.max())
               view_.hideProgress();
         }
      });

      events_.addHandler(ReplaceResultEvent.TYPE, new ReplaceResultEvent.Handler()
      {
         @Override
         public void onReplaceResult(ReplaceResultEvent event)
         {
            Debug.logToConsole("Replace Result Event with " + event.getResults().size() + " Results");
            if (event.getHandle() != currentFindHandle_)
               return;

            // toggle replace mode so matches get added to context
            if (view_.getReplaceMode())
                view_.toggleReplaceMode();

            view_.setStopReplaceButtonVisible(false);

            ArrayList<FindResult> results = event.getResults();
            for (FindResult fr : results)
               fr.setReplaceIndicator();
            //view_.clearMatches();
            view_.addMatches(results);
            view_.toggleReplaceMode();
            
            view_.ensureVisible(true);
            view_.disableReplace();
         }
      });

      new JSObjectStateValue(GROUP_FIND_IN_FILES, KEY_DIALOG_STATE,
                             ClientState.PROJECT_PERSISTENT,
                             session.getSessionInfo().getClientState(),
                             false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value == null)
            {
               dialogState_ = null;
               return;
            }
            
            // convert project-relative path if needed
            boolean relative = false;
            if (value.hasKey("projectRelative"))
               relative = value.getBoolean("projectRelative");
            
            if (relative)
            {
               FileSystemItem projDir = session_.getSessionInfo().getActiveProjectDir();
               if (projDir != null)
               {
                  String projPath = projDir.getPath();
                  value.setString("path", projPath + value.getString("path"));
               }
            }
            
            dialogState_ = value.cast();
         }

         @Override
         protected JsObject getValue()
         {
            if (dialogState_ == null)
               return dialogState_.cast();
            
            JsObject object = dialogState_.<JsObject>cast().clone();
            
            // convert path to relative if path is project-relative
            FileSystemItem projDir = session_.getSessionInfo().getActiveProjectDir();
            if (projDir != null)
            {
               String path = dialogState_.getPath();
               String projPath = projDir.getPath();
               if ((path + "/").startsWith(projPath + "/"))
               {
                  object.setString("path", path.substring(projPath.length()));
                  object.setBoolean("projectRelative", true);
               }
            }
            
            return object;
         }
      };
   }

   public void initialize(FindInFilesState state)
   {
      view_.ensureVisible(false);

      currentFindHandle_ = state.getHandle();
      view_.clearMatches();
      view_.addMatches(state.getResults().toArrayList());

      updateSearchLabel(state.getInput(), state.getPath(), state.isRegex());

      if (state.isRunning())
         view_.setStopSearchButtonVisible(true);
      else
         events_.fireEvent(new FindOperationEndedEvent(state.getHandle()));
   }

   public void onFindInFiles(FindInFilesEvent event)
   {
      FindInFilesDialog dialog = new FindInFilesDialog(new OperationWithInput<FindInFilesDialog.State>()
      {
         @Override
         public void execute(final FindInFilesDialog.State input)
         {
            dialogState_ = input;

            stopAndClear();

            FileSystemItem searchPath =
                                      FileSystemItem.createDir(input.getPath());

            JsArrayString filePatterns = JsArrayString.createArray().cast();
            for (String pattern : input.getFilePatterns())
               filePatterns.push(pattern);

            // find result always starts with !replaceMode
            if (view_.getReplaceMode())
                view_.toggleReplaceMode();

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
                                    updateSearchLabel(input.getQuery(),
                                                      input.getPath(),
                                                      input.isRegex());
                                    view_.setStopSearchButtonVisible(true);

                                    super.onResponseReceived(handle);

                                    view_.ensureVisible(true);
                                 }
                              });
         }
      });

      if (!StringUtil.isNullOrEmpty(event.getSearchPattern()))
         dialog.setSearchPattern(event.getSearchPattern());
      
      if (dialogState_ == null)
      {
         dialog.setDirectory(
               session_.getSessionInfo().getActiveProjectDir() != null ?
               session_.getSessionInfo().getActiveProjectDir() :
               workbenchContext_.getCurrentWorkingDir());
      }
      else
      {
         dialog.setState(dialogState_);
      }

      dialog.showModal();
   }

   public void onDismiss()
   {
      stopAndClear();
      server_.clearFindResults(new VoidServerRequestCallback());
   }

   private void updateSearchLabel(String query, String path, boolean regex)
   {
      if (regex)
         query = "/" + query + "/";
      else
         query = "\"" + query + "\"";
      view_.updateSearchLabel(query, path);
   }

   private void updateSearchLabel(String query, String path, boolean regex,
      String replace, boolean replaceRegex)
   {
      if (regex)
         query = "/" + query + "/";
      else
         query = "\"" + query + "\"";

      if (replaceRegex)
         replace = "/" + replace + "/";
      else
         replace = "\"" + replace + "\"";
      view_.updateSearchLabel(query, path, replace);
   }

   private void stopAndClear()
   {
      stop();
      stopReplace();
      view_.clearMatches();
      view_.clearSearchLabel();
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
   
   private void stopReplace()
   {
      if (currentFindHandle_ != null)
      {
         Debug.logToConsole("Notifying backend to stop replace");
         server_.stopReplace(currentFindHandle_,
                             new VoidServerRequestCallback());
         currentFindHandle_ = null;
         view_.setStopReplaceButtonVisible(false);
      }
   }

   private String currentFindHandle_;

   private FindInFilesDialog.State dialogState_;

   private final Display view_;
   private final FindInFilesServerOperations server_;
   private final Session session_;
   private final WorkbenchContext workbenchContext_;
   private EventBus events_;

   private static final String GROUP_FIND_IN_FILES = "find-in-files";
   private static final String KEY_DIALOG_STATE = "dialog-state";
   private GlobalDisplay globalDisplay_ = RStudioGinjector.INSTANCE.getGlobalDisplay();
}
