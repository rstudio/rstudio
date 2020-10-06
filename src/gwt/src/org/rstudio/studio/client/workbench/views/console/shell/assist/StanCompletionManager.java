/*
 * StanCompletionManager.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsVector;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.SignatureToolTipManager;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public class StanCompletionManager extends CompletionManagerBase
                                   implements CompletionManager
{
   public StanCompletionManager(DocDisplay docDisplay,
                                CompletionPopupDisplay popup,
                                CodeToolsServerOperations server,
                                CompletionContext context)
   {
      super(popup, docDisplay, server, context);
      context_ = context;
      
      sigTips_ = new SignatureToolTipManager(docDisplay)
      {
         @Override
         protected boolean isEnabled(Position position)
         {
            return DocumentMode.isPositionInStanMode(docDisplay, position);
         }
         
         @Override
         protected void getFunctionArguments(final String name,
                                             final String source,
                                             final String helpHandler,
                                             final CommandWithArg<String> onReady)
         {
            server_.stanGetArguments(name, new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String response)
               {
                  onReady.execute(response);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
         }
      };
   }
   
   @Override
   public void showAdditionalHelp(QualifiedName completion)
   {
      // NYI
   }
   
   @Override
   public boolean getCompletions(String line, CompletionRequestContext context)
   {
      server_.stanGetCompletions(line, context);
      return true;
   }
   
   @Override
   protected void addExtraCompletions(String token,
                                      List<QualifiedName> completions)
   {
      Set<String> discoveredIdentifiers = new HashSet<String>();
      
      Token cursorToken = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      TokenIterator it = docDisplay_.createTokenIterator();
      for (Token t = it.getCurrentToken();
           t != null && t != cursorToken;
           t = it.stepForward())
      {
         if (!t.hasType("identifier"))
            continue;
         
         String value = t.getValue();
         if (discoveredIdentifiers.contains(value))
            continue;
         
         if (!StringUtil.isSubsequence(value, token))
            continue;
         
         QualifiedName name = new QualifiedName(
               t.getValue(),
               "[identifier]",
               false,
               RCompletionType.CONTEXT,
               "",
               null,
               "Stan");
         
         discoveredIdentifiers.add(value);
         completions.add(name);
      }
   }
   
   @Override
   protected void onCompletionInserted(QualifiedName completion)
   {
      int type = completion.type;
      if (!RCompletionType.isFunctionType(type))
         return;
      
      boolean insertParensAfterCompletion =
            RCompletionType.isFunctionType(type) &&
            userPrefs_.insertParensAfterFunctionCompletion().getValue();
      
      if (insertParensAfterCompletion)
      {
         String meta = completion.meta;
         
         boolean isZeroArityFunction = meta.equals("()");
         if (!isZeroArityFunction)
         {
            docDisplay_.moveCursorBackward();
            sigTips_.displayToolTip(completion.name, completion.source, completion.helpHandler);
         }
      }
   }
   
   @Override
   public void goToHelp()
   {
      // TODO Auto-generated method stub
   }

   @Override
   public void goToDefinition()
   {
      // TODO Auto-generated method stub
   }

   private void runDiagnostics(final boolean useSourceDatabase)
   {
      // TODO: can we enable this in R Markdown documents using Stan?
      if (!docDisplay_.getFileType().isStan())
         return;
      
      server_.stanRunDiagnostics(
            context_.getPath(),
            useSourceDatabase,
            new ServerRequestCallback<JsArray<AceAnnotation>>()
            {
               @Override
               public void onResponseReceived(JsArray<AceAnnotation> response)
               {
                  JsVector<LintItem> lintItems = JsVector.createVector();
                  for (int i = 0; i < response.length(); i++)
                  {
                     LintItem item = createLintItem(response.get(i), useSourceDatabase);
                     if (item != null)
                        lintItems.push(item);
                  }
                  docDisplay_.showLint(lintItems.cast());
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private LintItem createLintItem(AceAnnotation annotation,
                                   boolean didUseSourceDatabase)
   {
      TokenIterator it = docDisplay_.createTokenIterator();
      Token token = it.moveToPosition(
            Position.create(annotation.row(), annotation.column()),
            false);
      if (token == null)
         return null;
      
      // for some reason, stan often marks the point _after_ an error
      // occurs rather than the location of the error itself, so try
      // moving backwards one token in those cases
      if (!token.hasType("identifier"))
      {
         token = it.stepBackward();
         if (token == null)
            return null;
      }
      
      // if this token is the same one at the cursor position,
      // don't use it
      if (didUseSourceDatabase)
      {
         Token cursorToken = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
         if (token.equals(cursorToken))
            return null;
      }

      return LintItem.create(
            annotation.row(),
            token.getColumn(),
            annotation.row(),
            token.getColumn() + token.getValue().length(),
            annotation.text(),
            annotation.type());
   }
   
   protected HandlerRegistration[] handlers()
   {
      return new HandlerRegistration[] {

            docDisplay_.addFocusHandler((FocusEvent event) -> {
               runDiagnostics(true);
            }),

            docDisplay_.addSaveCompletedHandler((SaveFileEvent event) -> {
               boolean useSourceDatabase = event.isAutosave();
               runDiagnostics(useSourceDatabase);
            })
            
      };
   }
   
   @Override
   public void detach()
   {
      sigTips_.detach();
   }
   
   private final CompletionContext context_;
   
   private final SignatureToolTipManager sigTips_;
}
