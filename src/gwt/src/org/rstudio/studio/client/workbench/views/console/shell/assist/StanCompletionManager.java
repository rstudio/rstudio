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

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;

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
      
      TokenIterator it = docDisplay_.createTokenIterator();
      for (Token t = it.getCurrentToken();
           t != null;
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
               RCompletionType.CONTEXT);
         discoveredIdentifiers.add(value);
         completions.add(name);
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
}
