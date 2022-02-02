/*
 * LintManager.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.RetinaStyleInjector;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintSource;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionOperation;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionRequest;
import org.rstudio.studio.client.workbench.views.source.editors.text.yaml.YamlDocumentLinter;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;

import java.util.List;

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
      if (type.isC() || type.isCpp())
         return userPrefs_.showDiagnosticsCpp().getValue();
      
      if (type.isR() || type.isRnw() || type.isRpres() || type.isMarkdown())
         return userPrefs_.showDiagnosticsR().getValue() || userPrefs_.realTimeSpellchecking().getValue();
      
      if (type.isYaml())
         return userPrefs_.showDiagnosticsYaml().getValue();
      
      return false;
   }

   public LintManager(LintSource source, List<HandlerRegistration> releaseOnDismiss)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      source_ = source;
      cppCompletionContext_ = source.getCppCompletionContext();
      docDisplay_ = source.getDisplay();
      yamlLinter_ = new YamlDocumentLinter(source.getRCompletionContext(), docDisplay_);
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
            if (!userPrefs_.backgroundDiagnostics().getValue())
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
                  // only remove spelling markers at cursor position
                  docDisplay_.removeMarkersAtCursorPosition();
                  showMarkers_ = false;
                  excludeCurrentStatement_ = true;
                  explicit_ = false;
                  timer_.schedule(defaultLintDelayMs());
               }
            });
         }
      });

      releaseOnDismiss.add(eventBus_.addHandler(
            SourceFileSaveCompletedEvent.TYPE,
            new SourceFileSaveCompletedEvent.Handler()
      {
         @Override
         public void onSourceFileSaveCompleted(
               SourceFileSaveCompletedEvent event)
         {
            // Skip if source doesn't want to be linted on save (this can change based on
            // the source's focus status so we have to check every time)
            if (!source_.lintOnSave())
               return;
            
            if (userPrefs_.diagnosticsOnSave().getValue())
               lint(false, true, false);
         }
      }));
   }

   public void relintAfterDelay(int delayMills)
   {
      timer_.schedule(delayMills == DEFAULT_LINT_DELAY ? defaultLintDelayMs() : delayMills);
   }

   @Inject
   void initialize(LintServerOperations server,
                   UserPrefs uiPrefs,
                   EventBus eventBus)
   {
      server_ = server;
      userPrefs_ = uiPrefs;
      eventBus_ = eventBus;
   }
   
   private int defaultLintDelayMs()
   { 
      return userPrefs_.backgroundDiagnosticsDelayMs().getValue();
   }
   
   private void lintActiveDocument(final LintContext context)
   {
      // don't lint if this is an unsaved document
      if (source_.getPath() == null)
         return;

      source_.withSavedDocument(context.showMarkers, () ->
      {
         performLintServerRequest(context);
      });
   }

   private void performLintServerRequest(final LintContext context)
   {
      if (context.token.isInvalid())
         return;

      if (userPrefs_.showDiagnosticsCpp().getValue() && (source_.getTextFileType().isCpp() || source_.getTextFileType().isC()))
         performCppLintServerRequest(context);
      else if (userPrefs_.showDiagnosticsR().getValue() && (source_.getTextFileType().isR() || source_.getTextFileType().isRmd()))
         performRLintServerRequest(context);
      else if (userPrefs_.showDiagnosticsYaml().getValue() && (source_.getTextFileType().isYaml()))
         performYamlLintRequest(context);
      else if (userPrefs_.realTimeSpellchecking().getValue())
         showLint(context, JsArray.createArray().cast());
   }

   private void performCppLintServerRequest(final LintContext context)
   {
      cppCompletionContext_.cppCompletionOperation(new CppCompletionOperation(){

         @Override
         public void execute(String docPath, int line, int column)
         {
            server_.getCppDiagnostics(
            source_.getPath(),
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
                        source_.getId(),
                        source_.getPath(),
                        StringUtil.notNull(source_.getCode()),
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
      });
   }

   private void performRLintServerRequest(final LintContext context)
   {

      server_.lintRSourceDocument(
            source_.getId(),
            source_.getPath(),
            StringUtil.notNull(source_.getCode()),
            context.showMarkers,
            context.explicit,
            new ServerRequestCallback<JsArray<LintItem>>()
            {
               @Override
               public void onResponseReceived(JsArray<LintItem> lint)
               {
                  if (context.token.isInvalid())
                     return;
                  
                  // lint yaml for rmd files and R chunks within rmd files
                  boolean isRmd = docDisplay_.getFileType().isRmd();
                  boolean isRmdRChunk = docDisplay_.getEditorBehavior().equals(EditorBehavior.AceBehaviorEmbedded) &&
                        docDisplay_.getFileType().isR();                  
                  if ((isRmd || isRmdRChunk) && userPrefs_.showDiagnosticsYaml().getValue())
                  {
                     yamlLinter_.getLint(context.explicit, yamlLint -> {
                        JsArray<LintItem> allLint = JsArray.createArray().cast();
                        for (int i = 0; i < lint.length(); i++)
                           allLint.push(lint.get(i));
                        for (int i = 0; i < yamlLint.length(); i++)
                           allLint.push(yamlLint.get(i));
                        showLint(context, allLint);
                     });               
                  }
                  else
                  {
                     showLint(context, lint);
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void performYamlLintRequest(final LintContext context)
   {
      yamlLinter_.getLint(context.explicit, lint -> {
         showLint(context, lint, false);
      });
   }
   
   private void showLint(LintContext context, JsArray<LintItem> lint)
   {
      showLint(context, lint, true);
   }
   
   private void showLint(LintContext context, JsArray<LintItem> lint, boolean spellcheck)
   {
      if (docDisplay_.isPopupVisible())
         return;

      JsArray<LintItem> finalLint;

      // Filter out items at the last cursor position, if the cursor hasn't moved.
      if (context.excludeCurrentStatement && docDisplay_.getCursorPosition().isEqualTo(context.cursorPosition))
      {
         finalLint = JsArray.createArray().cast();
         Position pos = context.cursorPosition;
         for (int i = 0; i < lint.length(); i++)
            if (!lint.get(i).asRange().contains(pos))
               finalLint.push(lint.get(i));
      }
      else
         finalLint = lint;

      for (int i = 0; i < finalLint.length(); i++) {
         LintItem lintItem = finalLint.get(i);
         DivElement element = Document.get().createDivElement();
         VirtualConsole vc = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(element);

         vc.submit(lintItem.getText());
         String renderedText = element.getInnerHTML();

         lintItem.setText(renderedText);
      }

      if (spellcheck && userPrefs_.realTimeSpellchecking().getValue())
      {
         source_.getSpellingTarget().getLint(new ServerRequestCallback<JsArray<LintItem>>()
         {
            @Override
            public void onResponseReceived(JsArray<LintItem> response)
            {
               for (int i = 0; i < response.length(); i++)
                  finalLint.push(response.get(i));

               source_.showLint(finalLint);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
      else
         source_.showLint(finalLint);
   }

   /**
    * Schedule a lint operation.
    *
    * @param milliseconds The number of milliseconds to delay before linting.
    */
   public void schedule(int milliseconds)
   {
      timer_.schedule(milliseconds);
   }

   /**
    * Cancel a pending lint operation, if any.
    */
   public void cancelPending()
   {
      if (timer_ != null && timer_.isRunning())
      {
         timer_.cancel();
      }
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

   public final static int DEFAULT_LINT_DELAY = -1;

   private final Timer timer_;
   private final LintSource source_;
   private final DocDisplay docDisplay_;
   private final Invalidation invalidation_;
   
   private boolean explicit_;
   private boolean showMarkers_;
   private boolean excludeCurrentStatement_;
   
   private LintServerOperations server_;
   private UserPrefs userPrefs_;
   private EventBus eventBus_;
   private final CppCompletionContext cppCompletionContext_;
   private final YamlDocumentLinter yamlLinter_;
   
   static {
      LintResources.INSTANCE.styles().ensureInjected();
      RetinaStyleInjector.injectAtEnd(
            LintResources.INSTANCE.retinaStyles().getText());
   }
}
