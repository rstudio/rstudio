/*
 * YamlCompletionManager.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import java.util.ArrayList;
import java.util.Collections;

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.DocumentMode.Mode;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

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
      // do we have a source that can handle this context
      YamlCompletionSource source = null;
      for (int i=0; i<sources_.length; i++) 
      {
         if (sources_[i].isActive(context_))
         {
            source = sources_[i];
            break;
         }
      }
      if (source == null)
         return false;
     
   
      // determine location (file | front-matter | cell)
      String location = null;
      if (DocumentMode.getModeForCursorPosition(docDisplay_) == Mode.YAML)
      {
         if (docDisplay_.getFileType().isRmd() ||
             (docDisplay_.getEditorBehavior() == EditorBehavior.AceBehaviorEmbedded))
         {
            location = "front-matter";
         }
         else
         {
            location = "file";
         }
      }
      else
      {
         location = "cell";
      }
      
      // determine code and cursor position
      Position pos = docDisplay_.getCursorPosition();
      String code = docDisplay_.getCode();
      
      // call for completions
      YamlCompletionParams params = YamlCompletionParams.create(location, line, code, pos);
      source.getCompletions(params, (res) -> {
         
         // default "empty" completion response
         String token = "";
         boolean cacheable = false;
         boolean suggestOnAccept = false;
         ArrayList<String> values = new ArrayList<String>();
         ArrayList<String> descriptions = new ArrayList<String>();
         
         // fill from result if we got one
         if (res != null)
         {
            YamlCompletionResult result = Js.uncheckedCast(res);
            token = result.getToken();
            for (int i=0; i<result.getCompletions().length(); i++)
            {
               values.add(result.getCompletions().get(i).getValue());
               descriptions.add(result.getCompletions().get(i).getDescription());
            }
            cacheable = result.getCacheable();
            suggestOnAccept = result.getSuggestOnAccept();
         }
          
         // create and send back response
         Completions response = Completions.createCompletions(
               token,
               JsUtil.toJsArrayString(values),
               JsUtil.toJsArrayString(new ArrayList<>(values.size())),
               JsUtil.toJsArrayBoolean(new ArrayList<>(values.size())),
               JsUtil.toJsArrayInteger(Collections.nCopies(values.size(), RCompletionType.YAML)),
               JsUtil.toJsArrayString(descriptions),
               "",
               true,
               true,
               true,
               null,
               null);         
         response.setCacheable(cacheable);
         response.setSuggestOnAccept(suggestOnAccept);
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
   

   private final CompletionContext context_;
   
   private final YamlCompletionSource[] sources_ = new YamlCompletionSource[] {
      new YamlCompletionSourceQuarto()
   };
}
