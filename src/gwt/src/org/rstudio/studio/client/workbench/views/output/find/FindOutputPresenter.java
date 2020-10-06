/*
 * FindOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.events.FindInFilesEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.PreviewReplaceEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceProgressEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceOperationEndedEvent;
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

      HandlerRegistration addSelectionChangedHandler(SelectionChangedEvent.Handler handler);

      void showOverflow();

      void showSearchCompleted();

      void updateSearchLabel(String query, String path);
      void updateSearchLabel(String query, String path, String replace);
      void updateSearchLabel(String query, String path, String replace, int successCount,
        int errorCount);
      void clearSearchLabel();

      boolean getRegexPreviewMode();
      boolean getReplaceMode();
      void setRegexPreviewMode(boolean value);
      void setReplaceMode(boolean value);
      HasClickHandlers getReplaceAllButton();
      String getReplaceText();

      HasClickHandlers getStopReplaceButton();
      void setStopReplaceButtonVisible(boolean visible);
      void enableReplace();
      void disableReplace();

      void showProgress();
      void hideProgress();
      ProgressBar getProgress();
   }

   public interface Binder extends CommandBinder<Commands, FindOutputPresenter> {}

   @Inject
   public FindOutputPresenter(Display view,
                              Binder binder,
                              Commands commands,
                              GlobalDisplay globalDisplay,
                              EventBus events,
                              FindInFilesServerOperations server,
                              final FileTypeRegistry ftr,
                              Session session,
                              WorkbenchContext workbenchContext,
                              FilesServerOperations fileServer)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      binder.bind(commands, this);
      events_ = events;
      server_ = server;
      session_ = session;
      workbenchContext_ = workbenchContext;
      fileServer_ = fileServer;

      view_.addSelectionChangedHandler(selectionChangedEvent ->
      {
         view_.ensureSelectedRowIsVisible();
      });

      view_.addSelectionCommitHandler((SelectionCommitEvent<CodeNavigationTarget> event) ->
      {
         CodeNavigationTarget target = event.getSelectedItem();
         if (target == null)
            return;

         ftr.editFile(FileSystemItem.createFile(target.getFile()),
                      target.getPosition());
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
            if (!StringUtil.equals(event.getHandle(), currentFindHandle_))
               return;

            view_.ensureVisible(true);
            {
               int count = 0;
               for (FindResult fr : event.getResults())
               {
                  count += fr.getMatchOns().size();
                  if (view_.getRegexPreviewMode())
                     fr.setRegexPreviewIndicator();
               }
               dialogState_.updateResultsCount(count);
            }
            view_.addMatches(event.getResults());
         }
      });

      events_.addHandler(FindOperationEndedEvent.TYPE, new FindOperationEndedEvent.Handler()
      {
         @Override
         public void onFindOperationEnded(
               FindOperationEndedEvent event)
         {
            if (StringUtil.equals(event.getHandle(), currentFindHandle_))
            {
               if (view_.getProgress().isVisible()) // check if a replace is in progress
                  events_.fireEvent(new ReplaceOperationEndedEvent(currentFindHandle_));
               else
                  currentFindHandle_ = null;
               view_.setStopSearchButtonVisible(false);
               view_.showSearchCompleted();
               // replace may have been previously disabled
               view_.enableReplace();
               view_.setRegexPreviewMode(dialogState_.isRegex());
            }
         }
      });

      events_.addHandler(PreviewReplaceEvent.TYPE, new PreviewReplaceEvent.Handler()
      {
         @Override
         public void onPreviewReplace(PreviewReplaceEvent event)
         {
            view_.setRegexPreviewMode(true);
            stopAndClear();
            dialogState_.clearResultsCount();

            FileSystemItem searchPath =
                                      FileSystemItem.createDir(dialogState_.getPath());
            JsArrayString includeFilePatterns = JsArrayString.createArray().cast();
            for (String pattern : dialogState_.getFilePatterns())
               includeFilePatterns.push(pattern);
            JsArrayString excludeFilePatterns = JsArrayString.createArray().cast();
            for (String pattern : dialogState_.getExcludeFilePatterns())
               excludeFilePatterns.push(pattern);

            server_.previewReplace(dialogState_.getQuery(),
                                   dialogState_.isRegex(),
                                   !dialogState_.isCaseSensitive(),
                                   searchPath,
                                   includeFilePatterns,
                                   excludeFilePatterns,
                                   view_.getReplaceText(),
                                   new SimpleRequestCallback<String>()
                                   {
                                      @Override
                                      public void onResponseReceived(String handle)
                                      {
                                         view_.clearMatches();
                                         currentFindHandle_ = handle;
                                         if (dialogState_ != null)
                                            dialogState_.clearResultsCount();
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
            if (dialogState_ == null)
               return;

            String message = "Are you sure you wish to permanently replace all? This will ";
            if (StringUtil.isNullOrEmpty(view_.getReplaceText()))
               message += "remove ";
            else
               message += "replace ";
            message += dialogState_.getResultsCount() +
                       " occurrences of '" +
                       dialogState_.getQuery() + "'";
            if (dialogState_.isRegex() || StringUtil.isNullOrEmpty(view_.getReplaceText()))
               message += " and cannot be undone.";
            else
               message += " with '" + view_.getReplaceText() +
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
                        JsArrayString includeFilePatterns = JsArrayString.createArray().cast();
                        for (String pattern : dialogState_.getFilePatterns())
                           includeFilePatterns.push(pattern);
                        JsArrayString excludeFilePatterns = JsArrayString.createArray().cast();
                        for (String pattern : dialogState_.getExcludeFilePatterns())
                           excludeFilePatterns.push(pattern);

                        server_.completeReplace(dialogState_.getQuery(),
                                                dialogState_.isRegex(),
                                                !dialogState_.isCaseSensitive(),
                                                searchPath,
                                                includeFilePatterns,
                                                excludeFilePatterns,
                                                dialogState_.getResultsCount(),
                                                view_.getReplaceText(),
                                                new SimpleRequestCallback<String>()
                                                {
                                                   @Override
                                                   public void onResponseReceived(String handle)
                                                   {
                                                      currentFindHandle_ = handle;
                                                      updateSearchLabel(dialogState_.getQuery(),
                                                                        dialogState_.getPath(),
                                                                        dialogState_.isRegex(),
                                                                        view_.getReplaceText());
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
            view_.showProgress();
            view_.getProgress().setProgress(event.replacedCount(), event.totalReplaceCount());
         }
      });

      events_.addHandler(ReplaceResultEvent.TYPE, new ReplaceResultEvent.Handler()
      {
         @Override
         public void onReplaceResult(ReplaceResultEvent event)
         {
            if (!StringUtil.equals(event.getHandle(), currentFindHandle_))
               return;

            // toggle replace mode so matches get added to context
            view_.setReplaceMode(true);

            ArrayList<FindResult> results = event.getResults();
            int errorCount = 0;
            for (FindResult fr : results)
            {
               fr.setReplaceIndicator();
               if (!StringUtil.isNullOrEmpty(fr.getErrors()))
               {
                  errorCount++;
               }
               dialogState_.updateReplaceErrors(fr.getErrors());
            }
            dialogState_.updateErrorCount(errorCount);

            view_.setReplaceMode(false);
            view_.addMatches(results);
            view_.setReplaceMode(true);

            view_.ensureVisible(true);
            view_.disableReplace();
         }
      });

      events_.addHandler(ReplaceOperationEndedEvent.TYPE, new ReplaceOperationEndedEvent.Handler()
      {
         @Override
         public void onReplaceOperationEnded(
               ReplaceOperationEndedEvent event)
         {
            if (StringUtil.equals(event.getHandle(), currentFindHandle_))
            {
               currentFindHandle_ = null;
               view_.hideProgress();
               view_.setStopReplaceButtonVisible(false);
               updateSearchLabel(dialogState_.getQuery(), dialogState_.getPath(),
                  dialogState_.isRegex(), view_.getReplaceText(), dialogState_.getErrorCount(),
                  dialogState_.getResultsCount());
            }
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
            dialogState_.clearResultsCount();

            FileSystemItem searchPath =
                                      FileSystemItem.createDir(input.getPath());

            JsArrayString includeFilePatterns = JsArrayString.createArray().cast();
            for (String pattern : input.getFilePatterns())
               includeFilePatterns.push(pattern);
            JsArrayString excludeFilePatterns = JsArrayString.createArray().cast();
            for (String pattern : input.getExcludeFilePatterns())
               excludeFilePatterns.push(pattern);

            // find result always starts with !replaceMode
            view_.setReplaceMode(false);

            view_.disableReplace();
            server_.beginFind(input.getQuery(),
                              input.isRegex(),
                              !input.isCaseSensitive(),
                              searchPath,
                              includeFilePatterns,
                              excludeFilePatterns,
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

      dialog.getDirectoryChooser().addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (session_.getSessionInfo().isVcsAvailable(VCSConstants.GIT_ID))
            {
               fileServer_.isGitDirectory(dialog.getDirectory(),
                                          new ServerRequestCallback<Boolean>() {
                  @Override
                  public void onResponseReceived(Boolean isGitDirectory)
                  {
                     dialog.setGitStatus(isGitDirectory);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     // assume true if we are not sure
                     // if the user enters invalid data it will be handled by the backend
                     dialog.setGitStatus(true);
                     Debug.logError(error);
                  }
               });
            }

            fileServer_.isPackageDirectory(dialog.getDirectory(),
                                           new ServerRequestCallback<Boolean>()
            {
               @Override
               public void onResponseReceived(final Boolean isPackageDirectory)
               {
                  dialog.setPackageStatus(isPackageDirectory);
               }
               @Override
               public void onError(ServerError error)
               {
                  // assume true if we are not sure
                  // if the user enters invalid data it will be handled by the backend
                  dialog.setPackageStatus(true);
                  Debug.logError(error);
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

   @Handler
   public void onActivateFindInFiles()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      view_.bringToFront();
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
      String replace)
   {
      if (regex)
      {
         query = "/" + query + "/";
         replace = "/" + replace + "/";
      }
      else
      {
         query = "\"" + query + "\"";
         replace = "\"" + replace + "\"";
      }

      view_.updateSearchLabel(query, path, replace);
   }

   private void updateSearchLabel(String query, String path, boolean regex,
      String replace, int errorCount, int resultsCount)
   {
      if (regex)
      {
         query = "/" + query + "/";
         replace = "/" + replace + "/";
      }
      else
      {
         query = "\"" + query + "\"";
         replace = "\"" + replace + "\"";
      }
      int successCount = resultsCount - errorCount;
      view_.updateSearchLabel(query, path, replace, successCount, errorCount);
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
         server_.stopReplace(currentFindHandle_,
                             new VoidServerRequestCallback());
         currentFindHandle_ = null;
         view_.setStopReplaceButtonVisible(false);
         view_.hideProgress();
      }
   }

   private String currentFindHandle_;
   private FindInFilesDialog.State dialogState_;

   private final Display view_;
   private final FindInFilesServerOperations server_;
   private final Session session_;
   private final WorkbenchContext workbenchContext_;
   private final FilesServerOperations fileServer_;
   private final Commands commands_;
   private final EventBus events_;

   private static final String GROUP_FIND_IN_FILES = "find-replace-in-files";
   private static final String KEY_DIALOG_STATE = "dialog-state";
   private final GlobalDisplay globalDisplay_;
}
