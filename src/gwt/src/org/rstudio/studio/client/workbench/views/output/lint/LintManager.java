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
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.inject.Inject;

public class LintManager
{
   public LintManager(Source source)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      source_ = source;
      invalidation_ = new Invalidation();
      timer_ = new Timer()
      {
         
         @Override
         public void run()
         {
            invalidation_.invalidate();
            Invalidation.Token token =
                  invalidation_.getInvalidationToken();
            lintActiveDocument(token);
         }
      };
      
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONKEYDOWN)
            {
               schedule(1000);
            }
         }
      });
   }
   
   @Inject
   void initialize(LintServerOperations server)
   {
      server_ = server;
   }
   
   private void lintActiveDocument(final Invalidation.Token token)
   {
      EditingTarget editor = source_.getActiveEditor();
      if (editor == null || !(editor instanceof TextEditingTarget))
         return;
      
      final TextEditingTarget target = (TextEditingTarget) editor;
      final DocDisplay docDisplay = target.getDocDisplay();
      final String documentId = editor.getId();
      
      // TODO: For C++, we display lint when completions are returned,
      // so we don't fire the timer here.
      if (target.getTextFileType().isCpp())
         return;
      
      target.withSavedDoc(new Command()
      {
         @Override
         public void execute()
         {
            performLintServerRequest(
                  token,
                  documentId,
                  docDisplay);
         }
      });
   }

   private void performLintServerRequest(final Invalidation.Token token,
                                         final String documentId,
                                         final DocDisplay docDisplay)
   {
      
      if (token.isInvalid())
         return;
      
      server_.lintRSourceDocument(
            documentId,
            false,
            new ServerRequestCallback<JsArray<LintItem>>()
            {
               @Override
               public void onResponseReceived(JsArray<LintItem> lint)
               {
                  if (token.isInvalid())
                     return;
                  
                  displayLint(docDisplay, lint);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });

   }
   
   public void displayLint(DocDisplay display,
                           JsArray<LintItem> lint)
   {
      display.showLint(lint);
   }
   
   public void schedule(int milliseconds)
   {
      timer_.schedule(milliseconds);
   }
   
   private final Timer timer_;
   private final Source source_;
   private final Invalidation invalidation_;
   
   private LintServerOperations server_;
   
   static {
      LintResources.INSTANCE.styles().ensureInjected();
   }
}
