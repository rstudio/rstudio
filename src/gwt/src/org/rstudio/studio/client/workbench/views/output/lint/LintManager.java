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
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionRequest;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class LintManager
{
   class LintContext
   {
      public LintContext(Invalidation.Token token,
            boolean showMarkers)
      {
         this.token = token;
         this.showMarkers = showMarkers;
      }
      
      public final Invalidation.Token token;
      public final boolean showMarkers;
   }
   
   private void reset()
   {
      showMarkers_ = false;
   }
   
   public LintManager(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      target_ = target;
      docDisplay_ = target.getDocDisplay();
      showMarkers_ = false;
      invalidation_ = new Invalidation();
      timer_ = new Timer()
      {
         
         @Override
         public void run()
         {
            invalidation_.invalidate();
            LintContext context = new LintContext(
                  invalidation_.getInvalidationToken(),
                  showMarkers_);
            reset();
            lintActiveDocument(context);
         }
      };
      
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            timer_.schedule(3000);
         }
      });
   }
   
   @Inject
   void initialize(LintServerOperations server)
   {
      server_ = server;
   }
   
   private void lintActiveDocument(final LintContext context)
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

   private void performLintServerRequest(final LintContext context)
   {
      
      if (context.token.isInvalid())
         return;
      
      if (target_.getTextFileType().isCpp())
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
                        context.showMarkers,
                        new ServerRequestCallback<JsArray<LintItem>>()
                        {
                           @Override
                           public void onResponseReceived(JsArray<LintItem> rLint)
                           {
                              if (context.token.isInvalid())
                                 return;
                              
                              JsArray<LintItem> lint = cppLint;
                              for (int i = 0; i < rLint.length(); i++)
                                 lint.push(rLint.get(i));
                              
                              docDisplay_.showLint(lint);
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
            context.showMarkers,
            new ServerRequestCallback<JsArray<LintItem>>()
            {
               @Override
               public void onResponseReceived(JsArray<LintItem> lint)
               {
                  if (context.token.isInvalid())
                     return;

                  docDisplay_.showLint(lint);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   public void schedule(int milliseconds)
   {
      timer_.schedule(milliseconds);
   }
   
   public void lint(boolean showMarkers)
   {
      showMarkers_ = true;
      timer_.schedule(0);
   }
   
   private final Timer timer_;
   private final TextEditingTarget target_;
   private final DocDisplay docDisplay_;
   private final Invalidation invalidation_;
   private boolean showMarkers_;
   
   private LintServerOperations server_;
   
   static {
      LintResources.INSTANCE.styles().ensureInjected();
   }
}
