/*
 * CompletionRequester.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchOracle;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager.AutocompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.RFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.DplyrJoinContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.RScopeObject;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions.RnwOptionCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CompletionRequester
{
   private CodeToolsServerOperations server_ ;
   private UIPrefs uiPrefs_;
   private final DocDisplay docDisplay_ ;
   private final SnippetHelper snippets_ ;

   private String cachedLinePrefix_ ;
   private HashMap<String, CompletionResult> cachedCompletions_ =
         new HashMap<String, CompletionResult>();
   private RnwCompletionContext rnwContext_ ;
   
   public CompletionRequester(RnwCompletionContext rnwContext,
                              DocDisplay docDisplay,
                              SnippetHelper snippets)
   {
      rnwContext_ = rnwContext;
      docDisplay_ = docDisplay;
      snippets_ = snippets;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(CodeToolsServerOperations server, UIPrefs uiPrefs)
   {
      server_ = server;
      uiPrefs_ = uiPrefs;
   }
   
   private boolean usingCache(
         String token,
         final ServerRequestCallback<CompletionResult> callback)
   {
      return usingCache(token, false, callback);
   }
   
   private boolean usingCache(
         String token,
         boolean isHelpCompletion,
         final ServerRequestCallback<CompletionResult> callback)
   {
      if (isHelpCompletion)
         token = token.substring(token.lastIndexOf(':') + 1);
      
      if (cachedLinePrefix_ == null)
         return false;
      
      CompletionResult cachedResult = cachedCompletions_.get("");
      if (cachedResult == null)
         return false;
      
      if (token.toLowerCase().startsWith(cachedLinePrefix_.toLowerCase()))
      {
         String diff = token.substring(cachedLinePrefix_.length(), token.length());

         // if we already have a cached result for this diff, use it
         CompletionResult cached = cachedCompletions_.get(diff);
         if (cached != null)
         {
            callback.onResponseReceived(cached);
            return true;
         }

         // otherwise, produce a new completion list
         if (diff.length() > 0 && !diff.endsWith("::"))
         {
            callback.onResponseReceived(narrow(token, diff, cachedResult)) ;
            return true;
         }
      }
      
      return false;
      
   }
   
   private String basename(String absolutePath)
   {
      return absolutePath.substring(absolutePath.lastIndexOf('/') + 1);
   }
   
   private boolean filterStartsWithDot(String item,
                                       String token)
   {
      return !(!token.startsWith(".") && item.startsWith("."));
   }
   
   private static final native String fuzzy(String string) /*-{
      return string.replace(/(?!^)[._]/g, "");
   }-*/;
   
   private CompletionResult narrow(final String token,
                                   final String diff,
                                   CompletionResult cachedResult)
   {
      ArrayList<QualifiedName> newCompletions = new ArrayList<QualifiedName>();
      newCompletions.ensureCapacity(cachedResult.completions.size());
      
      // For completions that are files or directories, we need to post-process
      // the token and the qualified name to strip out just the basename (filename).
      // Note that we normalize the paths such that files will have no trailing slash,
      // while directories will have one trailing slash (but we defend against multiple
      // trailing slashes)
      
      // Transform the token once beforehand for completions.
      final String tokenSub   = token.substring(token.lastIndexOf('/') + 1);
      final String tokenFuzzy = fuzzy(tokenSub);
      
      for (QualifiedName qname : cachedResult.completions)
      {
         // File types are narrowed only by the file name
         if (RCompletionType.isFileType(qname.type))
         {
            if (StringUtil.isSubsequence(basename(qname.name), tokenFuzzy, true))
               newCompletions.add(qname);
         }
         else
         {
            if (StringUtil.isSubsequence(qname.name, tokenFuzzy, true) &&
                filterStartsWithDot(qname.name, token))
               newCompletions.add(qname) ;
         }
      }
      
      java.util.Collections.sort(newCompletions, new Comparator<QualifiedName>() {
         
         @Override
         public int compare(QualifiedName lhs, QualifiedName rhs)
         {
            int lhsScore = RCompletionType.isFileType(lhs.type)
                  ? CodeSearchOracle.scoreMatch(basename(lhs.name), tokenSub, true)
                  : CodeSearchOracle.scoreMatch(lhs.name, token, false);
            
            int rhsScore = RCompletionType.isFileType(rhs.type)
               ? CodeSearchOracle.scoreMatch(basename(rhs.name), tokenSub, true)
               : CodeSearchOracle.scoreMatch(rhs.name, token, false);
            
            // Place arguments higher (give less penalty)
            if (lhs.type == RCompletionType.ARGUMENT) lhsScore -= 3;
            if (rhs.type == RCompletionType.ARGUMENT) rhsScore -= 3;
            
            if (lhsScore == rhsScore)
               return lhs.compareTo(rhs);
            
            return lhsScore < rhsScore ? -1 : 1;
         }
      });
      
      CompletionResult result = new CompletionResult(
            token,
            newCompletions,
            cachedResult.guessedFunctionName,
            cachedResult.suggestOnAccept,
            cachedResult.dontInsertParens) ;
      
      cachedCompletions_.put(diff, result);
      return result;
   }
   
   public void getDplyrJoinCompletionsString(
         final String token,
         final String string,
         final String cursorPos,
         final boolean implicit,
         final ServerRequestCallback<CompletionResult> callback)
   {
      if (usingCache(token, callback))
         return;
      
      server_.getDplyrJoinCompletionsString(
            token,
            string,
            cursorPos,
            new ServerRequestCallback<Completions>() {
               
               @Override
               public void onResponseReceived(Completions response)
               {
                  cachedLinePrefix_ = token;
                  fillCompletionResult(response, implicit, callback);
               }

               @Override
               public void onError(ServerError error)
               {
                  callback.onError(error);
               }

            });
      
      
   }
   
   public void getDplyrJoinCompletions(
         final DplyrJoinContext joinContext,
         final boolean implicit,
         final ServerRequestCallback<CompletionResult> callback)
   {
      final String token = joinContext.getToken();
      if (usingCache(token, callback))
         return;
      
      server_.getDplyrJoinCompletions(
            joinContext.getToken(),
            joinContext.getLeftData(),
            joinContext.getRightData(),
            joinContext.getVerb(),
            joinContext.getCursorPos(),
            new ServerRequestCallback<Completions>() {

               @Override
               public void onError(ServerError error)
               {
                  callback.onError(error);
               }
               
               @Override
               public void onResponseReceived(Completions response)
               {
                  cachedLinePrefix_ = token;
                  fillCompletionResult(response, implicit, callback);
               }
               
            });
   }
   
   private void fillCompletionResult(
         Completions response,
         boolean implicit,
         ServerRequestCallback<CompletionResult> callback)
   {
      JsArrayString comp = response.getCompletions();
      JsArrayString pkgs = response.getPackages();
      JsArrayBoolean quote = response.getQuote();
      JsArrayInteger type = response.getType();
      ArrayList<QualifiedName> newComp = new ArrayList<QualifiedName>();
      for (int i = 0; i < comp.length(); i++)
      {
         newComp.add(new QualifiedName(comp.get(i), pkgs.get(i), quote.get(i), type.get(i), response.getHelpHandler()));
      }

      CompletionResult result = new CompletionResult(
            response.getToken(),
            newComp,
            response.getGuessedFunctionName(),
            response.getSuggestOnAccept(),
            response.getOverrideInsertParens());

      if (response.isCacheable())
      {
         cachedCompletions_.put("", result);
      }

      if (!implicit || result.completions.size() != 0)
         callback.onResponseReceived(result);

   }
   
   private static final Pattern RE_EXTRACTION = Pattern.create("[$@:]", "");
   private boolean isTopLevelCompletionRequest()
   {
      String line = docDisplay_.getCurrentLineUpToCursor();
      return !RE_EXTRACTION.test(line);
   }
   
   public void getCompletions(
         final String token,
         final List<String> assocData,
         final List<Integer> dataType,
         final List<Integer> numCommas,
         final String functionCallString,
         final String chainDataName,
         final JsArrayString chainAdditionalArgs,
         final JsArrayString chainExcludeArgs,
         final boolean chainExcludeArgsFromObject,
         final String filePath,
         final String documentId,
         final String line,
         final boolean implicit,
         final ServerRequestCallback<CompletionResult> callback)
   {
      boolean isHelp = dataType.size() > 0 &&
            dataType.get(0) == AutocompletionContext.TYPE_HELP;
      
      if (usingCache(token, isHelp, callback))
         return;
      
      doGetCompletions(
            token,
            assocData,
            dataType,
            numCommas,
            functionCallString,
            chainDataName,
            chainAdditionalArgs,
            chainExcludeArgs,
            chainExcludeArgsFromObject,
            filePath,
            documentId,
            line,
            new ServerRequestCallback<Completions>()
      {
         @Override
         public void onError(ServerError error)
         {
            callback.onError(error);
         }

         @Override
         public void onResponseReceived(Completions response)
         {
            cachedLinePrefix_ = token;
            String token = response.getToken();

            JsArrayString comp = response.getCompletions();
            JsArrayString pkgs = response.getPackages();
            JsArrayBoolean quote = response.getQuote();
            JsArrayInteger type = response.getType();
            ArrayList<QualifiedName> newComp = new ArrayList<QualifiedName>();
            
            // Get function completions from the server
            for (int i = 0; i < comp.length(); i++)
               if (comp.get(i).endsWith(" = "))
                  newComp.add(new QualifiedName(comp.get(i), pkgs.get(i), quote.get(i), type.get(i), response.getHelpHandler()));
            
            // Try getting our own function argument completions
            if (!response.getExcludeOtherCompletions())
            {
               addFunctionArgumentCompletions(token, newComp);
               addScopedArgumentCompletions(token, newComp);
            }
            
            // Get variable completions from the current scope
            if (!response.getExcludeOtherCompletions())
            {
               addScopedCompletions(token, newComp, "variable");
               addScopedCompletions(token, newComp, "function");
            }
            
            // Get other server completions
            for (int i = 0; i < comp.length(); i++)
               if (!comp.get(i).endsWith(" = "))
                  newComp.add(new QualifiedName(comp.get(i), pkgs.get(i), quote.get(i), type.get(i), response.getHelpHandler()));
            
            // Get snippet completions. Bail if this isn't a top-level
            // completion -- TODO is to add some more context that allows us
            // to properly ascertain this.
            if (isTopLevelCompletionRequest())
               addSnippetCompletions(token, newComp);
            
            // Remove duplicates
            newComp = resolveDuplicates(newComp);
            
            CompletionResult result = new CompletionResult(
                  response.getToken(),
                  newComp,
                  response.getGuessedFunctionName(),
                  response.getSuggestOnAccept(),
                  response.getOverrideInsertParens());

            if (response.isCacheable())
            {
               cachedCompletions_.put("", result);
            }

            callback.onResponseReceived(result);
         }
      }) ;
   }
   
   private ArrayList<QualifiedName>
   resolveDuplicates(ArrayList<QualifiedName> completions)
   {
      ArrayList<QualifiedName> result =
            new ArrayList<QualifiedName>();
      result.addAll(completions);
      
      // sort the results by name and type for efficient processing
      Collections.sort(completions, new Comparator<QualifiedName>()
      {
         @Override
         public int compare(QualifiedName o1, QualifiedName o2)
         {
            int name = o1.name.compareTo(o2.name);
            if (name != 0)
               return name;
            return o1.type - o2.type;
         }
      });
      
      // walk backwards through the list and remove elements which have the 
      // same name and type
      for (int i = completions.size() - 1; i > 0; i--)
      {
         QualifiedName o1 = completions.get(i);
         QualifiedName o2 = completions.get(i - 1);
         
         // remove qualified names which have the same name and type (allow
         // shadowing of contextual results to reduce confusion)
         if (o1.name == o2.name && 
             (o1.type == o2.type || o1.type == RCompletionType.CONTEXT))
            result.remove(o1);
      }
      
      return result;
   }
   
   private void addScopedArgumentCompletions(
         String token,
         ArrayList<QualifiedName> completions)
   {
      AceEditor editor = (AceEditor) docDisplay_;

      // NOTE: this will be null in the console, so protect against that
      if (editor != null)
      {
         Position cursorPosition =
               editor.getSession().getSelection().getCursor();
         CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
         JsArray<RFunction> scopedFunctions =
               codeModel.getFunctionsInScope(cursorPosition);
         
         if (scopedFunctions.length() == 0)
            return;
         
         String tokenLower = token.toLowerCase();
         
         for (int i = 0; i < scopedFunctions.length(); i++)
         {
            RFunction scopedFunction = scopedFunctions.get(i);
            String functionName = scopedFunction.getFunctionName();

            JsArrayString argNames = scopedFunction.getFunctionArgs();
            for (int j = 0; j < argNames.length(); j++)
            {
               String argName = argNames.get(j);
               if (argName.toLowerCase().startsWith(tokenLower))
               {
                  if (functionName == null || functionName == "")
                  {
                     completions.add(new QualifiedName(
                           argName,
                           "<anonymous function>",
                           false,
                           RCompletionType.CONTEXT
                     ));
                  }
                  else
                  {
                     completions.add(new QualifiedName(
                           argName,
                           functionName,
                           false,
                           RCompletionType.CONTEXT
                     ));
                  }
               }
            }
         } 
      }
   }
      
   private void addScopedCompletions(
         String token,
         ArrayList<QualifiedName> completions,
         String type)
   {
      AceEditor editor = (AceEditor) docDisplay_;

      // NOTE: this will be null in the console, so protect against that
      if (editor != null)
      {
         Position cursorPosition =
               editor.getSession().getSelection().getCursor();
         CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
      
         JsArray<RScopeObject> scopeVariables =
               codeModel.getVariablesInScope(cursorPosition);
         
         String tokenLower = token.toLowerCase();
         for (int i = 0; i < scopeVariables.length(); i++)
         {
            RScopeObject variable = scopeVariables.get(i);
            if (variable.getType() == type &&
                variable.getToken().toLowerCase().startsWith(tokenLower))
               completions.add(new QualifiedName(
                     variable.getToken(),
                     variable.getType(),
                     false,
                     RCompletionType.CONTEXT
               ));
         }
      }
   }
   
   private void addFunctionArgumentCompletions(
         String token,
         ArrayList<QualifiedName> completions)
   {
      AceEditor editor = (AceEditor) docDisplay_;

      if (editor != null)
      {
         Position cursorPosition =
               editor.getSession().getSelection().getCursor();
         CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
         
         // Try to see if we can find a function name
         TokenCursor cursor = codeModel.getTokenCursor();
         
         // NOTE: This can fail if the document is empty
         if (!cursor.moveToPosition(cursorPosition))
            return;
         
         String tokenLower = token.toLowerCase();
         if (cursor.currentValue() == "(" || cursor.findOpeningBracket("(", false))
         {
            if (cursor.moveToPreviousToken())
            {
               // Check to see if this really is the name of a function
               JsArray<ScopeFunction> functionsInScope =
                     codeModel.getAllFunctionScopes();
               
               String tokenName = cursor.currentValue();
               for (int i = 0; i < functionsInScope.length(); i++)
               {
                  ScopeFunction rFunction = functionsInScope.get(i);
                  String fnName = rFunction.getFunctionName();
                  if (tokenName == fnName)
                  {
                     JsArrayString args = rFunction.getFunctionArgs();
                     for (int j = 0; j < args.length(); j++)
                     {
                        String arg = args.get(j);
                        if (arg.toLowerCase().startsWith(tokenLower))
                           completions.add(new QualifiedName(
                                 args.get(j) + " = ",
                                 fnName,
                                 false,
                                 RCompletionType.CONTEXT
                           ));
                     }
                  }
               }
            }
         }
      }
   }
   
   private void addSnippetCompletions(
         String token,
         ArrayList<QualifiedName> completions)
   {
      if (StringUtil.isNullOrEmpty(token))
         return;
      
      if (uiPrefs_.enableSnippets().getValue())
      {
         ArrayList<String> snippets = snippets_.getAvailableSnippets();
         String tokenLower = token.toLowerCase();
         for (String snippet : snippets)
            if (snippet.toLowerCase().startsWith(tokenLower))
               completions.add(0, QualifiedName.createSnippet(snippet));
      }
   }

   private void doGetCompletions(
         final String token,
         final List<String> assocData,
         final List<Integer> dataType,
         final List<Integer> numCommas,
         final String functionCallString,
         final String chainObjectName,
         final JsArrayString chainAdditionalArgs,
         final JsArrayString chainExcludeArgs,
         final boolean chainExcludeArgsFromObject,
         final String filePath,
         final String documentId,
         final String line,
         final ServerRequestCallback<Completions> requestCallback)
   {
      int optionsStartOffset;
      if (rnwContext_ != null &&
          (optionsStartOffset = rnwContext_.getRnwOptionsStart(token, token.length())) >= 0)
      {
         doGetSweaveCompletions(token, optionsStartOffset, token.length(), requestCallback);
      }
      else
      {
         server_.getCompletions(
               token,
               assocData,
               dataType,
               numCommas,
               functionCallString,
               chainObjectName,
               chainAdditionalArgs,
               chainExcludeArgs,
               chainExcludeArgsFromObject,
               filePath,
               documentId,
               line,
               requestCallback);
      }
   }

   private void doGetSweaveCompletions(
         final String line,
         final int optionsStartOffset,
         final int cursorPos,
         final ServerRequestCallback<Completions> requestCallback)
   {
      rnwContext_.getChunkOptions(new ServerRequestCallback<RnwChunkOptions>()
      {
         @Override
         public void onResponseReceived(RnwChunkOptions options)
         {
            RnwOptionCompletionResult result = options.getCompletions(
                  line,
                  optionsStartOffset,
                  cursorPos,
                  rnwContext_ == null ? null : rnwContext_.getActiveRnwWeave());
            
            String[] pkgNames = new String[result.completions.length()];
            Arrays.fill(pkgNames, "`chunk-option`");

            Completions response = Completions.createCompletions(
                  result.token,
                  result.completions,
                  JsUtil.toJsArrayString(pkgNames),
                  JsUtil.toJsArrayBoolean(new ArrayList<Boolean>(result.completions.length())),
                  JsUtil.toJsArrayInteger(new ArrayList<Integer>(result.completions.length())),
                  "",
                  false,
                  false,
                  true,
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
      cachedCompletions_.clear();
   }
   
   public static class CompletionResult
   {
      public CompletionResult(String token,
                              ArrayList<QualifiedName> completions,
                              String guessedFunctionName,
                              boolean suggestOnAccept,
                              boolean dontInsertParens)
      {
         this.token = token ;
         this.completions = completions ;
         this.guessedFunctionName = guessedFunctionName ;
         this.suggestOnAccept = suggestOnAccept ;
         this.dontInsertParens = dontInsertParens ;
      }
      
      public final String token ;
      public final ArrayList<QualifiedName> completions ;
      public final String guessedFunctionName ;
      public final boolean suggestOnAccept ;
      public final boolean dontInsertParens ;
   }
   
   public static class QualifiedName implements Comparable<QualifiedName>
   {
      public QualifiedName(
            String name, String source, boolean shouldQuote, int type)
      {
         this(name, source, shouldQuote, type, null);
      }
      
      public QualifiedName(
            String name, String source, boolean shouldQuote, int type, String helpHandler)
      {
         this.name = name;
         this.source = source;
         this.shouldQuote = shouldQuote;
         this.type = type;
         this.helpHandler = helpHandler;
         
      }
      
      public QualifiedName(String name, String source)
      {
         this.name = name;
         this.source = source;
         this.shouldQuote = false;
         this.type = RCompletionType.UNKNOWN;
         this.helpHandler = null;
      }
      
      public static QualifiedName createSnippet(String name)
      {
         return new QualifiedName(
               name,
               "snippet",
               false,
               RCompletionType.SNIPPET,
               null);
      }
      
      @Override
      public String toString()
      {
         SafeHtmlBuilder sb = new SafeHtmlBuilder();
         
         // Get an icon for the completion
         // We use separate styles for file icons, so we can nudge them
         // a bit differently
         String style = RES.styles().completionIcon();
         if (RCompletionType.isFileType(type))
            style = RES.styles().fileIcon();
            
         SafeHtmlUtil.appendImage(
               sb,
               style,
               getIcon());
         
         // Get the display name. Note that for file completions this requires
         // some munging of the 'name' and 'package' fields.
         addDisplayName(sb);
         
         return sb.toSafeHtml().asString();
      }
      
      private void addDisplayName(SafeHtmlBuilder sb)
      {
         // Handle files specially
         if (RCompletionType.isFileType(type))
            doAddDisplayNameFile(sb);
         else
            doAddDisplayNameGeneric(sb);
      }
      
      private void doAddDisplayNameFile(SafeHtmlBuilder sb)
      {
         ArrayList<Integer> slashIndices =
               StringUtil.indicesOf(name, '/');

         if (slashIndices.size() < 1)
         {
            SafeHtmlUtil.appendSpan(
                  sb,
                  RES.styles().completion(),
                  name);
         }
         else
         {
            int lastSlashIndex = slashIndices.get(
                  slashIndices.size() - 1);

            int firstSlashIndex = 0;
            if (slashIndices.size() > 2)
               firstSlashIndex = slashIndices.get(
                     slashIndices.size() - 3);

            String endName = name.substring(lastSlashIndex + 1);
            String startName = "";
            if (slashIndices.size() > 2)
               startName += "...";
            startName += name.substring(firstSlashIndex, lastSlashIndex);

            SafeHtmlUtil.appendSpan(
                  sb,
                  RES.styles().completion(),
                  endName);

            SafeHtmlUtil.appendSpan(
                  sb,
                  RES.styles().packageName(),
                  startName);
         }

      }
      
      private void doAddDisplayNameGeneric(SafeHtmlBuilder sb)
      {
         // Get the name for the completion
         SafeHtmlUtil.appendSpan(
               sb,
               RES.styles().completion(),
               name);

         // Display the source for functions and snippets (unless there
         // is a custom helpHandler provided, indicating that the "source"
         // isn't a package but rather some custom DollarNames scope)
         if ((RCompletionType.isFunctionType(type) ||
             type == RCompletionType.SNIPPET) &&
             helpHandler == null)
         {
            SafeHtmlUtil.appendSpan(
                  sb,
                  RES.styles().packageName(),
                  "{" + source.replaceAll("package:", "") + "}");
         }
      }
      
      private ImageResource getIcon()
      {
         if (RCompletionType.isFunctionType(type))
            return new ImageResource2x(ICONS.function2x());
         
         switch(type)
         {
         case RCompletionType.UNKNOWN:
            return new ImageResource2x(ICONS.variable2x());
         case RCompletionType.VECTOR:
            return new ImageResource2x(ICONS.variable2x());
         case RCompletionType.ARGUMENT:
            return new ImageResource2x(ICONS.variable2x());
         case RCompletionType.ARRAY:
         case RCompletionType.DATAFRAME:
            return new ImageResource2x(ICONS.dataFrame2x());
         case RCompletionType.LIST:
            return new ImageResource2x(ICONS.clazz2x());
         case RCompletionType.ENVIRONMENT:
            return new ImageResource2x(ICONS.environment2x());
         case RCompletionType.S4_CLASS:
         case RCompletionType.S4_OBJECT:
         case RCompletionType.R5_CLASS:
         case RCompletionType.R5_OBJECT:
            return new ImageResource2x(ICONS.clazz2x());
         case RCompletionType.FILE:
            return getIconForFilename(name);
         case RCompletionType.DIRECTORY:
            return new ImageResource2x(ICONS.folder2x());
         case RCompletionType.CHUNK:
         case RCompletionType.ROXYGEN:
            return new ImageResource2x(ICONS.keyword2x());
         case RCompletionType.HELP:
            return new ImageResource2x(ICONS.help2x());
         case RCompletionType.STRING:
            return new ImageResource2x(ICONS.variable2x());
         case RCompletionType.PACKAGE:
            return new ImageResource2x(ICONS.rPackage2x());
         case RCompletionType.KEYWORD:
            return new ImageResource2x(ICONS.keyword2x());
         case RCompletionType.CONTEXT:
            return new ImageResource2x(ICONS.context2x());
         case RCompletionType.SNIPPET:
            return new ImageResource2x(ICONS.snippet2x());
         default:
            return new ImageResource2x(ICONS.variable2x());
         }
      }
      
      private ImageResource getIconForFilename(String name)
      {
         return FILE_TYPE_REGISTRY.getIconForFilename(name);
      }

      public static QualifiedName parseFromText(String val)
      {
         String name, pkgName = "";
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
         
         String pkg = source == null ? "" : source ;
         String opkg = o.source == null ? "" : o.source ;
         return pkg.compareTo(opkg) ;
      }
      
      @Override
      public boolean equals(Object object)
      {
         if (!(object instanceof QualifiedName))
            return false;
         
         QualifiedName other = (QualifiedName) object;
         return name.equals(other.name) &&
                type == other.type;
      }
      
      @Override
      public int hashCode()
      {
         int hash = 17;
         hash = 31 * hash + name.hashCode();
         hash = 31 * hash + type;
         return hash;
      }

      public final String name ;
      public final String source ;
      public final boolean shouldQuote ;
      public final int type ;
      public final String helpHandler;
      private static final FileTypeRegistry FILE_TYPE_REGISTRY =
            RStudioGinjector.INSTANCE.getFileTypeRegistry();
   }
   
   private static final CompletionRequesterResources RES =
         CompletionRequesterResources.INSTANCE;
   
   private static final CodeIcons ICONS = CodeIcons.INSTANCE;
   
   static {
      RES.styles().ensureInjected();
   }
   
}
