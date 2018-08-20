/*
 * StanCompletionManager.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.SignatureToolTipManager;

public class StanCompletionManager extends CompletionManagerBase
                                   implements CompletionManager
{
   public StanCompletionManager(DocDisplay docDisplay,
                                CompletionPopupDisplay popup,
                                CodeToolsServerOperations server,
                                CompletionContext context)
   {
      super(popup, docDisplay, context);
      
      docDisplay_ = docDisplay;
      popup_ = popup;
      server_ = server;
      context_ = context;
      
      sigTips_ = new SignatureToolTipManager(docDisplay)
      {
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
   public void getCompletions(String line, CompletionRequestContext context)
   {
      server_.stanGetCompletions(line, context);
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
            uiPrefs_.insertParensAfterFunctionCompletion().getValue();
      
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

   @Override
   public void codeCompletion()
   {
      // TODO Auto-generated method stub
      
   }

   private final DocDisplay docDisplay_;
   private final CompletionPopupDisplay popup_;
   private final CodeToolsServerOperations server_;
   private final CompletionContext context_;
   
   private final SignatureToolTipManager sigTips_;
}
