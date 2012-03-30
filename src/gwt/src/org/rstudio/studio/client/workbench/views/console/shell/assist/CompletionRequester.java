/*
 * CompletionRequester.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenizer;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions.AsyncProvider;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions.RnwOptionCompletionResult;

import java.util.ArrayList;


public class CompletionRequester
{
   private final CodeToolsServerOperations server_ ;
   private final AsyncProvider pRnwChunkOptions_;

   private String cachedLinePrefix_ ;
   private CompletionResult cachedResult_ ;
   
   public CompletionRequester(CodeToolsServerOperations server,
                              RnwChunkOptions.AsyncProvider pRnwChunkOptions)
   {
      server_ = server ;
      pRnwChunkOptions_ = pRnwChunkOptions;
   }
   
   public void getCompletions(
                     final String line, 
                     final int pos,
                     final boolean implicit,
                     final ServerRequestCallback<CompletionResult> callback)
   {
      if (cachedResult_ != null && cachedResult_.guessedFunctionName == null)
      {
         if (line.substring(0, pos).startsWith(cachedLinePrefix_))
         {
            String diff = line.substring(cachedLinePrefix_.length(), pos) ;
            if (diff.length() > 0)
            {
               ArrayList<RToken> tokens = RTokenizer.asTokens("a" + diff) ;
               
               // when we cross a :: the list may actually grow, not shrink
               if (!diff.endsWith("::"))
               {
                  while (tokens.size() > 0 
                        && tokens.get(tokens.size()-1).getContent().equals(":"))
                  {
                     tokens.remove(tokens.size()-1) ;
                  }
               
                  if (tokens.size() == 1
                        && tokens.get(0).getTokenType() == RToken.ID)
                  {
                     callback.onResponseReceived(narrow(diff)) ;
                     return ;
                  }
               }
            }
         }
      }
      
      doGetCompletions(line, pos, new ServerRequestCallback<Completions>()
      {
         @Override
         public void onError(ServerError error)
         {
            callback.onError(error);
         }

         @Override
         public void onResponseReceived(Completions response)
         {
            cachedLinePrefix_ = line.substring(0, pos);

            JsArrayString comp = response.getCompletions();
            JsArrayString pkgs = response.getPackages();
            ArrayList<QualifiedName> newComp = new ArrayList<QualifiedName>();

            for (int i = 0; i < comp.length(); i++)
               newComp.add(new QualifiedName(comp.get(i), pkgs.get(i)));

            CompletionResult result = new CompletionResult(
                  response.getToken(),
                  newComp,
                  response.getGuessedFunctionName(),
                  response.getSuggestOnAccept());

            cachedResult_ = response.isCacheable() ? result : null;

            if (!implicit || result.completions.size() != 0)
               callback.onResponseReceived(result);
         }
      }) ;
   }

   private void doGetCompletions(
         String line,
         int pos,
         ServerRequestCallback<Completions> requestCallback)
   {
      if (pRnwChunkOptions_ != null && line.matches("^<<.*"))
      {
         doGetSweaveCompletions(line, pos, requestCallback);
      }
      else
      {
         server_.getCompletions(line, pos, requestCallback);
      }
   }

   private void doGetSweaveCompletions(
         final String line,
         final int pos,
         final ServerRequestCallback<Completions> requestCallback)
   {
      pRnwChunkOptions_.getChunkOptions(new ServerRequestCallback<RnwChunkOptions>()
      {
         @Override
         public void onResponseReceived(RnwChunkOptions options)
         {
            RnwOptionCompletionResult result = options.getCompletions(line,
                                                                      pos);

            Completions response = Completions.createCompletions(
                  result.token,
                  result.completions,
                  JsUtil.createEmptyArray(result.completions.length())
                        .<JsArrayString>cast(),
                  null);
            // Unlike other completion types, Sweave completions are not
            // guaranteed to narrow the candidate list (in particular
            // true/false).
            response.setCacheable(false);
            if (result.completions.length() > 0 &&
                result.completions.get(0).endsWith("="))
            {
               response.setSuggestOnAccept(true);
            }
            requestCallback.onResponseReceived(response);
         }

         @Override
         public void onError(ServerError error)
         {
            requestCallback.onError(error);
         }
      });
   }

   public void flushCache()
   {
      cachedLinePrefix_ = null ;
      cachedResult_ = null ;
   }
   
   private CompletionResult narrow(String diff)
   {
      assert cachedResult_.guessedFunctionName == null ;
      
      String token = cachedResult_.token + diff ;
      ArrayList<QualifiedName> newCompletions = new ArrayList<QualifiedName>() ;
      for (QualifiedName qname : cachedResult_.completions)
         if (qname.name.startsWith(token))
            newCompletions.add(qname) ;
      
      return new CompletionResult(token, newCompletions, null,
                                  cachedResult_.suggestOnAccept) ;
   }

   public static class CompletionResult
   {
      public CompletionResult(String token, ArrayList<QualifiedName> completions,
                              String guessedFunctionName,
                              boolean suggestOnAccept)
      {
         this.token = token ;
         this.completions = completions ;
         this.guessedFunctionName = guessedFunctionName ;
         this.suggestOnAccept = suggestOnAccept ;
      }
      
      public final String token ;
      public final ArrayList<QualifiedName> completions ;
      public final String guessedFunctionName ;
      public final boolean suggestOnAccept ;
   }
   
   public static class QualifiedName implements Comparable<QualifiedName>
   {
      public QualifiedName(String name, String pkgName)
      {
         this.name = name ;
         this.pkgName = pkgName ;
      }
      
      @Override
      public String toString()
      {
         return DomUtils.textToHtml(name) + getFormattedPackageName();
      }

      private String getFormattedPackageName()
      {
         return pkgName == null || pkgName.length() == 0
               ? ""
               : " <span class=\"packageName\">{"
                  + DomUtils.textToHtml(pkgName)
                  + "}</span>";
      }

      public static QualifiedName parseFromText(String val)
      {
         String name, pkgName = null;
         int idx = val.indexOf('{') ;
         if (idx < 0)
         {
            name = val ;
         }
         else
         {
            name = val.substring(0, idx).trim() ;
            pkgName = val.substring(idx + 1, val.length() - 1) ;
         }
         
         return new QualifiedName(name, pkgName) ;
      }

      public int compareTo(QualifiedName o)
      {
         if (name.endsWith("=") ^ o.name.endsWith("="))
            return name.endsWith("=") ? -1 : 1 ;
         
         int result = String.CASE_INSENSITIVE_ORDER.compare(name, o.name) ;
         if (result != 0)
            return result ;
         
         String pkg = pkgName == null ? "" : pkgName ;
         String opkg = o.pkgName == null ? "" : o.pkgName ;
         return pkg.compareTo(opkg) ;
      }

      public final String name ;
      public final String pkgName ;
   }
}
