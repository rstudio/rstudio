/*
 * YamlCompletionManager.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManagerBase;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequestContext;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import jsinterop.base.Js;

public class YamlCompletionManager extends CompletionManagerBase
                                       implements CompletionManager
                                       
{
   // Use a funstructor to create an instance in order to ensure toggleHandlers()
   // is invoked after the object is fully instantiated
   public static YamlCompletionManager create(DocDisplay docDisplay,
                                                  CompletionPopupDisplay popup,
                                                  CodeToolsServerOperations server,
                                                  CompletionContext context)
   {
      YamlCompletionManager retVal = new YamlCompletionManager(docDisplay, popup, server, context);

      retVal.toggleHandlers(true);

      return retVal;
   }

   // Use the create() funstructor above instead of invoking this constructor directly
   private YamlCompletionManager(DocDisplay docDisplay,
                                    CompletionPopupDisplay popup,
                                    CodeToolsServerOperations server,
                                    CompletionContext context)
   {
      super(popup, docDisplay, server, context);
      context_ = context;
   }

   @Override
   public void goToHelp()
   {
      // NYI
   }

   @Override
   public void goToDefinition()
   {
      // NYI
     
   }
   
   @Override
   public void showAdditionalHelp(QualifiedName completion)
   {
      // NYI
   }

   @Override
   public boolean getCompletions(String line, CompletionRequestContext context)
   {  
      // do we have a provider that can handle this context
      YamlEditorToolsProvider provider = providers_.getActiveProvider(
         context_.getPath(), context_.getExtendedFileType()
      );
      if (provider == null)
         return false;
      
      // call for completions
      YamlEditorContext editorContext = YamlEditorContext.create(true, context_, docDisplay_);
      provider.getCompletions(editorContext, (res) -> {
         
         // default "empty" completion response
         String token = "";
         boolean cacheable = false;
        
         ArrayList<Integer> types = new ArrayList<Integer>();
         ArrayList<String> values = new ArrayList<String>();
         ArrayList<String> display = new ArrayList<String>();
         ArrayList<String> descriptions = new ArrayList<String>();
         ArrayList<Boolean> suggestOnAccept = new ArrayList<Boolean>();
         ArrayList<Boolean> replaceToEnd = new ArrayList<Boolean>();
         
         // fill from result if we got one
         if (res != null)
         {
            YamlCompletionResult result = Js.uncheckedCast(res);
            token = result.getToken();
            for (int i=0; i<result.getCompletions().length(); i++)
            {
               YamlCompletion completion = result.getCompletions().get(i);
               if ("key".equals(completion.getType()))
                  types.add(RCompletionType.YAML_KEY);
               else 
                  types.add(RCompletionType.YAML_VALUE);
               values.add(completion.getValue());
               display.add(completion.getDisplay());
               descriptions.add(completion.getDescription());
               suggestOnAccept.add(completion.getSuggestOnAccept());
               replaceToEnd.add(completion.getReplaceToEnd());
            }
            cacheable = result.getCacheable();
         }
          
         // create and send back response
         Completions response = Completions.createCompletions(
               token,
               JsUtil.toJsArrayString(values),
               JsUtil.toJsArrayString(display),
               JsUtil.toJsArrayString(new ArrayList<>(values.size())),
               JsUtil.toJsArrayBoolean(new ArrayList<>(values.size())),
               JsUtil.toJsArrayInteger(types),
               JsUtil.toJsArrayBoolean(suggestOnAccept),
               JsUtil.toJsArrayBoolean(replaceToEnd),
               JsUtil.toJsArrayString(descriptions),
               "",
               true,
               true,
               true,
               null,
               null);         
         response.setCacheable(cacheable);
         context.onResponseReceived(response); 
      });
      
      // will return completions
      return true;
   }
   
   @Override
   public boolean allowInComment()
   {
      return true;
   }
   
   @Override
   protected boolean isCompletionCharacter(char ch)
   {
      return !StringUtil.isWhitespace(ch) && (ch != ':');
   }
   
   private final YamlEditorToolsProviders providers_ = new YamlEditorToolsProviders();

   private final CompletionContext context_;

}
