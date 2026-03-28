/*
 * NotebookHtmlRenderer.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderPendingEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookCreateResult;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.NotebookRenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.StatusBarIconType;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class NotebookHtmlRenderer
             implements SaveFileEvent.Handler,
                        RmdRenderPendingEvent.Handler,
                        RenderRmdEvent.Handler,
                        NotebookRenderFinishedEvent.Handler
{
   public NotebookHtmlRenderer(DocDisplay display, TextEditingTarget target,
         TextEditingTarget.Display editingDisplay,
         DocUpdateSentinel sentinel, RMarkdownServerOperations server,
         EventBus events, DependencyManager dependencyManager)
   {
      display_ = display;
      editingDisplay_ = editingDisplay;
      target_ = target;
      server_ = server;
      sentinel_ = sentinel;
      events_ = events;
      dependencyManager_ = dependencyManager;

      registrations_ = new HandlerRegistrations(
         events_.addHandler(RmdRenderPendingEvent.TYPE, this),
         events_.addHandler(RenderRmdEvent.TYPE, this),
         events_.addHandler(NotebookRenderFinishedEvent.TYPE, this)
      );
   }

   public boolean isRendering()
   {
      return isRunning_;
   }

   public void addRenderCompleteHandler(Command callback)
   {
      // only the most recent caller's continuation matters; earlier ones
      // correspond to stale preview requests that were superseded
      renderCompleteHandler_ = callback;
   }

   public void onDismiss()
   {
      registrations_.detach();
      if (renderTimer_ != null)
      {
         renderTimer_.cancel();
         renderTimer_ = null;
      }
      if (isRunning_)
      {
         // clear state without invoking renderCompleteHandler_ -- the
         // editing target is being torn down so callbacks are invalid
         isRunning_ = false;
         renderCompleteHandler_ = null;
         clearStatus();
      }
   }

   @Override
   public void onRmdRenderPending(RmdRenderPendingEvent event)
   {
      if (event.getDocId() != sentinel_.getId())
         return;

      // wait up to a second for the R Markdown render to start before we
      // initiate a render of the notebook
      renderTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (renderCommand_ != null)
            {
               renderCommand_.execute();
            }
         }
      };
      renderTimer_.schedule(1000);
   }

   @Override
   public void onRenderRmd(RenderRmdEvent event)
   {
      if (renderTimer_ != null && renderTimer_.isRunning())
      {
         renderTimer_.run();
         renderTimer_.cancel();
         renderTimer_ = null;
      }
   }

   @Override
   public void onNotebookRenderFinished(NotebookRenderFinishedEvent event)
   {
      // only handle events for our document
      if (!sentinel_.getId().equals(event.getDocId()))
         return;

      // only handle if we initiated the render
      if (!isRunning_)
         return;

      if (!event.succeeded())
      {
         String errorMessage = event.getErrorMessage();
         if (errorMessage != null && !errorMessage.isEmpty())
         {
            // show error in status bar instead of clearing it
            cancelStatusMessage();
            editingDisplay_.getStatusBar().showStatus(
               StatusBarIconType.TYPE_ERROR,
               constants_.errorCreatingNotebookPrefix() + errorMessage);

            finishRender(false);
            return;
         }
      }

      finishRender();
   }

   @Override
   public void onSaveFile(SaveFileEvent event)
   {
      // bail if this was an autosave
      if (event.isAutosave())
         return;

      // bail if we don't render chunks inline (for safety--notebooks
      // are always in this mode)
      if (!display_.showChunkOutputInline())
         return;

      // bail if no notebook output format
      if (!target_.hasRmdNotebook())
         return;

      final String rmdPath = sentinel_.getPath();

      // bail if unsaved doc (no point in generating notebooks for those)
      if (StringUtil.isNullOrEmpty(rmdPath))
         return;

      final String outputPath =
            FilePathUtils.filePathSansExtension(rmdPath) +
            RmdOutput.NOTEBOOK_EXT;

      // if a render is already in progress, the C++ side will cancel it and
      // start a new one; reset our local state so we track the new render
      if (isRunning_)
      {
         isRunning_ = false;
         clearStatus();
      }

      // create the command to render the notebook
      Command renderCommand = new Command()
      {
         @Override
         public void execute()
         {
            isRunning_ = true;
            scheduleStatusMessage();
            createNotebookDeferred(rmdPath, outputPath);
         }
      };

      // if an R Markdown render is pending, wait for it to start before we ask
      // the notebook to render; this ensures that in the case where we're
      // rendering both the notebook and another format that the other format
      // starts first
      if (renderTimer_ != null && renderTimer_.isRunning())
      {
         renderCommand_ = renderCommand;
      }
      else
      {
         renderCommand.execute();
      }
   }

   // Private methods ---------------------------------------------------------

   private void scheduleStatusMessage()
   {
      cancelStatusMessage();
      statusTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (isRunning_)
            {
               editingDisplay_.getStatusBar().showStatus(
                  StatusBarIconType.TYPE_LOADING,
                  constants_.notebookRendering());
            }
         }
      };
      statusTimer_.schedule(1000);
   }

   private void cancelStatusMessage()
   {
      if (statusTimer_ != null)
      {
         statusTimer_.cancel();
         statusTimer_ = null;
      }
   }

   private void clearStatus()
   {
      cancelStatusMessage();
      editingDisplay_.getStatusBar().hideStatus();
   }

   private void finishRender()
   {
      finishRender(true);
   }

   private void finishRender(boolean shouldClearStatus)
   {
      isRunning_ = false;
      if (shouldClearStatus)
         clearStatus();

      // invoke and clear any pending completion callback
      if (renderCompleteHandler_ != null)
      {
         Command callback = renderCompleteHandler_;
         renderCompleteHandler_ = null;
         callback.execute();
      }
   }

   private void createNotebookDeferred(final String rmdPath,
                                       final String outputPath)
   {
      dependencyManager_.withUnsatisfiedDependencies(
            Dependency.cranPackage("rmarkdown", "1.6"),
            new ServerRequestCallback<JsArray<Dependency>>()
            {
               @Override
               public void onResponseReceived(JsArray<Dependency> unsatisfied)
               {
                  if (unsatisfied.length() == 0)
                  {
                     createNotebookFromCache(rmdPath, outputPath);
                     return;
                  }

                  Dependency dependency = unsatisfied.get(0);
                  String message;

                  if (StringUtil.isNullOrEmpty(dependency.getVersion()))
                  {
                     message = constants_.rMarkdownNotInstalledHTMLNoGenerate();
                  }
                  else
                  {
                     message = constants_.rMarkdownUpgradeRequired();
                  }

                  editingDisplay_.showWarningBar(message);
                  finishRender();
               }

               @Override
               public void onError(ServerError error)
               {
                  finishRender();
                  Debug.logError(error);
               }
            }
      );
   }

   private void createNotebookFromCache(final String rmdPath,
                                       final String outputPath)
   {
      CommandWithArg<Boolean> createNotebookCmd = new CommandWithArg<Boolean>()
      {
         final String warningPrefix = constants_.errorCreatingNotebookPrefix();
         @Override
         public void execute(Boolean metDependencies)
         {
            // dependencies not available, just turn off the isRunning flag so
            // we can try again next time
            if (!metDependencies)
            {
               isRunning_ = false;
               clearStatus();
               return;
            }

            server_.createNotebookFromCache(
                  rmdPath,
                  outputPath,
                  new ServerRequestCallback<NotebookCreateResult>()
                  {
                     @Override
                     public void onResponseReceived(NotebookCreateResult result)
                     {
                        if (!result.started())
                        {
                           // render was not started (already up to date or
                           // another render is in progress) -- clear state
                           isRunning_ = false;
                           clearStatus();
                        }
                        // if started == true, the async process is running;
                        // onNotebookRenderFinished will handle completion
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        editingDisplay_.showWarningBar(warningPrefix +
                              error.getMessage());
                        isRunning_ = false;
                        clearStatus();
                     }
                  });
         }
      };

      dependencyManager_.withRMarkdown(constants_.rNotebook(), constants_.creatingRNotebooks(),
            createNotebookCmd);
   }

   private boolean isRunning_;
   private Timer renderTimer_;
   private Timer statusTimer_;
   private Command renderCommand_;
   private Command renderCompleteHandler_;

   private final HandlerRegistrations registrations_;
   private final DocDisplay display_;
   private final TextEditingTarget target_;
   private final DocUpdateSentinel sentinel_;
   private final RMarkdownServerOperations server_;
   private final EventBus events_;
   private final TextEditingTarget.Display editingDisplay_;
   private final DependencyManager dependencyManager_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
