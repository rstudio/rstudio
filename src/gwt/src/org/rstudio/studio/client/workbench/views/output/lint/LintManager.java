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
import org.rstudio.core.client.Invalidation;
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
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class LintManager
{
   static class LintContext
   {
      public LintContext(Invalidation.Token token,
                         Position cursorPosition,
                         boolean showMarkers,
                         boolean explicit,
                         boolean excludeCurrentStatement)
      {
         this.cursorPosition = cursorPosition;
         this.token = token;
         this.showMarkers = showMarkers;
         this.explicit = explicit;
         this.excludeCurrentStatement = excludeCurrentStatement;
      }
      
      public final Invalidation.Token token;
      public final Position cursorPosition;
      public final boolean showMarkers;
      public final boolean explicit;
      public final boolean excludeCurrentStatement;
   }
   
   private void reset()
   {
      showMarkers_ = false;
      explicit_ = false;
      excludeCurrentStatement_ = true;
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
      showMarkers_ = false;
      explicit_ = false;
      invalidation_ = new Invalidation();
      timer_ = new Timer()
      {
         
         @Override
         public void run()
         {
            if (!isLintableDocument())
            {
               getAceWorkerDiagnostics(docDisplay_);
               return;
            }
            
            invalidation_.invalidate();
            LintContext context = new LintContext(
                  invalidation_.getInvalidationToken(),
                  docDisplay_.getCursorPosition(),
                  showMarkers_,
                  explicit_,
                  excludeCurrentStatement_);
            reset();
            lintActiveDocument(context);
         }
      };
      
      // Background linting
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            if (!uiPrefs_.enableBackgroundDiagnostics().getValue())
               return;
            
            if (!docDisplay_.isFocused())
               return;
            
            if (docDisplay_.isPopupVisible())
               return;
            
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  docDisplay_.removeMarkersOnCursorLine();
                  showMarkers_ = false;
                  excludeCurrentStatement_ = true;
                  explicit_ = false;
                  timer_.schedule(uiPrefs_.backgroundDiagnosticsDelayMs().getValue());
               }
            });
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
               lint(false, true, false);
         }
      });
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
   
   private void lintActiveDocument(final LintContext context)
   {
      // don't lint if this is an unsaved document
      if (target_.getPath() == null)
         return;
      
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
      if (context.token.isInvalid())
         return;
      
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
                  if (context.token.isInvalid())
                     return;
                  
                  final JsArray<LintItem> cppLint =
                        CppCompletionRequest.asLintArray(diag);
                  
                  server_.lintRSourceDocument(
                        target_.getId(),
                        target_.getPath(),
                        context.showMarkers,
                        context.explicit,
                        new ServerRequestCallback<JsArray<LintItem>>()
                        {
                           @Override
                           public void onResponseReceived(JsArray<LintItem> rLint)
                           {
                              if (context.token.isInvalid())
                                 return;
                              
                              JsArray<LintItem> allLint = JsArray.createArray().cast();
                              for (int i = 0; i < cppLint.length(); i++)
                                 allLint.push(cppLint.get(i));
                              for (int i = 0; i < rLint.length(); i++)
                                 allLint.push(rLint.get(i));
                              showLint(context, allLint);
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                           }
                        });
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }

   private void performRLintServerRequest(final LintContext context)
   {

      server_.lintRSourceDocument(
            target_.getId(),
            target_.getPath(),
            context.showMarkers,
            context.explicit,
            new ServerRequestCallback<JsArray<LintItem>>()
            {
               @Override
               public void onResponseReceived(JsArray<LintItem> lint)
               {
                  if (context.token.isInvalid())
                     return;

                  showLint(context, lint);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
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
         Position pos = context.cursorPosition;
         JsArray<LintItem> filteredLint = JsArray.createArray().cast();
         for (int i = 0; i < lint.length(); i++)
            if (!lint.get(i).asRange().contains(pos))
               filteredLint.push(lint.get(i));
         
         docDisplay_.showLint(filteredLint);
         return;
      }
      
      docDisplay_.showLint(lint);
      return;
      
   }
   
   public void schedule(int milliseconds)
   {
      timer_.schedule(milliseconds);
   }
   
   public void lint(boolean showMarkers,
                    boolean explicit,
                    boolean excludeCurrentStatement)
   {
      showMarkers_ = showMarkers;
      explicit_ = explicit;
      excludeCurrentStatement_ = excludeCurrentStatement;
      
      // Add tiny delay to ensure lint not cleared by other concurrent events
      timer_.schedule(20);
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
      // The 'timeout' here is a bit of a hack to ensure lint is not immediately
      // cleared from other events.
      var callback = $entry(function() {
         var worker = editor.getSession().$worker;
         if (worker) worker.update();
      });
      $wnd.setTimeout(callback, 100);
   }-*/;
   
   private final Timer timer_;
   private final TextEditingTarget target_;
   private final DocDisplay docDisplay_;
   private final Invalidation invalidation_;
   
   private boolean explicit_;
   private boolean showMarkers_;
   private boolean excludeCurrentStatement_;
   
   private LintServerOperations server_;
   private UIPrefs uiPrefs_;
   private EventBus eventBus_;
   
   static {
      LintResources.INSTANCE.styles().ensureInjected();
      RetinaStyleInjector.injectAtEnd(
            LintResources.INSTANCE.retinaStyles().getText());
   }
}
