/*
 * LintPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.lint;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.RetinaStyleInjector;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionRequest;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class LintManager
{
   static class LintContext
   {
      public LintContext(Position cursorPosition,
                         boolean showMarkers,
                         boolean excludeCurrentStatement)
      {
         this.cursorPosition = cursorPosition;
         this.showMarkers = showMarkers;
         this.excludeCurrentStatement = excludeCurrentStatement;
      }
      
      public final Position cursorPosition;
      public final boolean showMarkers;
      public final boolean excludeCurrentStatement;
   }
   
   class BackgroundLinter
   {
      public BackgroundLinter(DocDisplay docDisplay,
                              int pollingIntervalMs,
                              int idleTime)
      {
         docDisplay_ = docDisplay;
         pollingIntervalMs_ = pollingIntervalMs;
         idleTime_ = idleTime;
         isRunning_ = false;
         cancellationRequested_ = false;
         immediateLintRequested_ = false;
         modifiedTimeAtLastLintRequest_ = 0;
         lintCommand_ = new RepeatingCommand()
         {
            
            @Override
            public boolean execute()
            {
               if (cancellationRequested_)
                  return cancel();
               
               if (isRunning_)
                  return true;
               
               long lastModifiedTime = docDisplay_.getLastModifiedTime();
               if (lastModifiedTime == modifiedTimeAtLastLintRequest_)
                  return true;
               
               if (!immediateLintRequested_)
               {
                  long timeDelta =
                        System.currentTimeMillis() -
                        lastModifiedTime;

                  if (timeDelta < idleTime_)
                     return true;
               }
               
               isRunning_ = true;
               modifiedTimeAtLastLintRequest_ = lastModifiedTime;
               return performBackgroundLintTask();
            }
         };
      }
      
      private boolean onFinishedLintTask()
      {
         signalTaskCompleted();
         return true;
      }
      
      public void start()
      {
         Scheduler.get().scheduleFixedDelay(lintCommand_, pollingIntervalMs_);
      }
      
      public void updatePollingInterval(int pollingIntervalMs)
      {
         requestCancellation();
         pollingIntervalMs_ = pollingIntervalMs;
         start();
      }
      
      public void requestCancellation()
      {
         cancellationRequested_ = true;
      }
      
      public void signalTaskCompleted()
      {
         isRunning_ = false;
         immediateLintRequested_ = false;
      }
      
      private boolean cancel()
      {
         isRunning_ = false;
         cancellationRequested_ = false;
         return false;
      }
      
      public void requestImmediateLint()
      {
         immediateLintRequested_ = true;
         lintCommand_.execute();
      }
      
      private boolean performBackgroundLintTask()
      {
         if (!isLintableDocument())
         {
            getAceWorkerDiagnostics(docDisplay_);
            return true;
         }

         if (!docDisplay_.isFocused())
            return onFinishedLintTask();
         
         if (docDisplay_.isPopupVisible())
            return onFinishedLintTask();
         
         lintActiveDocument(new LintContext(
               docDisplay_.getCursorPosition(),
               false,
               true));

         return true;
      }
      
      private void lintActiveDocument(final LintContext context)
      {
         // don't lint if this is an unsaved document
         if (target_.getPath() == null)
         {
            onFinishedLintTask();
            return;
         }

         if (context.showMarkers)
         {
            target_.save(new Command()
            {
               @Override
               public void execute()
               {
                  performLintServerRequest(context);
               }
            });
         }
         else
         {
            target_.withSavedDoc(new Command()
            {
               @Override
               public void execute()
               {
                  performLintServerRequest(context);
               }
            });
         }
      }

      private void performLintServerRequest(final LintContext context)
      {
         if (target_.getTextFileType().isCpp() ||
               target_.getTextFileType().isC())
            performCppLintServerRequest(context);
         else
            performRLintServerRequest(context);
      }

      private void performCppLintServerRequest(final LintContext context)
      {
         server_.getCppDiagnostics(
               target_.getPath(),
               new ServerRequestCallback<JsArray<CppDiagnostic>>()
               {

                  @Override
                  public void onResponseReceived(JsArray<CppDiagnostic> diag)
                  {
                     final JsArray<LintItem> cppLint =
                           CppCompletionRequest.asLintArray(diag);

                     server_.lintRSourceDocument(
                           target_.getId(),
                           target_.getPath(),
                           context.showMarkers,
                           new ServerRequestCallback<JsArray<LintItem>>()
                           {
                              @Override
                              public void onResponseReceived(JsArray<LintItem> rLint)
                              {
                                 JsArray<LintItem> allLint = JsArray.createArray().cast();
                                 for (int i = 0; i < cppLint.length(); i++)
                                    allLint.push(cppLint.get(i));
                                 for (int i = 0; i < rLint.length(); i++)
                                    allLint.push(rLint.get(i));
                                 showLint(context, allLint);
                                 onFinishedLintTask();
                              }

                              @Override
                              public void onError(ServerError error)
                              {
                                 Debug.logError(error);
                                 onFinishedLintTask();
                              }
                           });
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     onFinishedLintTask();
                  }
               });
      }

      private void performRLintServerRequest(final LintContext context)
      {

         server_.lintRSourceDocument(
               target_.getId(),
               target_.getPath(),
               context.showMarkers,
               new ServerRequestCallback<JsArray<LintItem>>()
               {
                  @Override
                  public void onResponseReceived(JsArray<LintItem> lint)
                  {
                     showLint(context, lint);
                     onFinishedLintTask();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     onFinishedLintTask();
                  }
               });
      }

      private void showLint(LintContext context,
            JsArray<LintItem> lint)
      {
         if (docDisplay_.isPopupVisible() || !docDisplay_.isFocused())
            return;

         // Filter out items at the last cursor position, if the cursor
         // hasn't moved.
         if (context.excludeCurrentStatement &&
               docDisplay_.getCursorPosition().isEqualTo(context.cursorPosition))
         {
            int startRow = docDisplay_.getStartOfCurrentStatement();
            int endRow = docDisplay_.getEndOfCurrentStatement();

            if (startRow != -1 && endRow != -1)
            {
               JsArray<LintItem> filteredLint = JsArray.createArray().cast();
               for (int i = 0; i < lint.length(); i++)
               {
                  if (lint.get(i).getStartRow() < startRow ||
                        lint.get(i).getStartRow() > endRow)
                  {
                     filteredLint.push(lint.get(i));
                  }
               }
               docDisplay_.showLint(filteredLint);
               return;
            }
         }

         docDisplay_.showLint(lint);
         return;

      }
   
      private void getAceWorkerDiagnostics(final DocDisplay docDisplay)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               AceEditor editor = (AceEditor) docDisplay;
               if (editor != null)
                  doGetAceWorkerDiagnostics(editor.getWidget().getEditor());
            }
         });
      }

      private final native void doGetAceWorkerDiagnostics(AceEditorNative editor) /*-{
         // The 'timeout' here is a bit of a hack to ensure
         // lint is not immediately cleared from other events.
         setTimeout(function() {
            var worker = editor.getSession().$worker;
            if (worker)
               worker.update();
         }, 50);
      }-*/;
      
      private final DocDisplay docDisplay_;
      private int pollingIntervalMs_;
      private int idleTime_;
      private RepeatingCommand lintCommand_;
      private boolean isRunning_;
      private boolean cancellationRequested_;
      private boolean immediateLintRequested_;
      private long modifiedTimeAtLastLintRequest_;
      
   }
   
   // NOTE: by 'lintable' we mean 'uses RStudio-internal' linter
   // rather than Ace worker
   private boolean isLintableDocument()
   {
      TextFileType type = docDisplay_.getFileType();
      return (((type.isC() || type.isCpp()) && uiPrefs_.showDiagnosticsCpp().getValue()) ||
              ((type.isR() || type.isRmd() || type.isRnw() || type.isRpres()) && uiPrefs_.showDiagnosticsR().getValue()));
   }
   
   public LintManager(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      target_ = target;
      docDisplay_ = target.getDocDisplay();
      linter_ = new BackgroundLinter(
            docDisplay_,
            100,
            uiPrefs_.backgroundDiagnosticsDelayMs().getValue());
      
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            docDisplay_.removeMarkersOnCursorLine();
         }
      });
      
      eventBus_.addHandler(
            SourceFileSaveCompletedEvent.TYPE,
            new SourceFileSaveCompletedEvent.Handler()
      {
         @Override
         public void onSourceFileSaveCompleted(
               SourceFileSaveCompletedEvent event)
         {
            if (!docDisplay_.isFocused())
               return;
            
            if (uiPrefs_.diagnosticsOnSave().getValue())
               linter_.requestImmediateLint();
         }
      });
      
      if (uiPrefs_.enableBackgroundDiagnostics().getValue())
         linter_.start();
   }
   
   @Inject
   void initialize(LintServerOperations server,
                   UIPrefs uiPrefs,
                   EventBus eventBus)
   {
      server_ = server;
      uiPrefs_ = uiPrefs;
      eventBus_ = eventBus;
   }
   
   public void showDiagnostics()
   {
      linter_.requestImmediateLint();
   }
   
   private final TextEditingTarget target_;
   private final DocDisplay docDisplay_;
   private final BackgroundLinter linter_;
   
   private LintServerOperations server_;
   private UIPrefs uiPrefs_;
   private EventBus eventBus_;
   
   static {
      LintResources.INSTANCE.styles().ensureInjected();
      RetinaStyleInjector.injectAtEnd(
            LintResources.INSTANCE.retinaStyles().getText());
   }
}
