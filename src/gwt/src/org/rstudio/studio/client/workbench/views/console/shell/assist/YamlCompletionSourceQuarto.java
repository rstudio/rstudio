/*
 * YamlCompletionSourceQuarto.java
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.inject.Inject;

import elemental2.core.JsObject;
import elemental2.promise.IThenable;
import elemental2.promise.Promise;
import elemental2.promise.IThenable.ThenOnFulfilledCallbackFn;
import elemental2.promise.IThenable.ThenOnRejectedCallbackFn;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


public class YamlCompletionSourceQuarto implements YamlCompletionSource
{

   public YamlCompletionSourceQuarto()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(Session session)
   {
      config_ = session.getSessionInfo().getQuartoConfig();
   }
   
   @Override
   public boolean isActive(CompletionContext context)
   {
      if (config_.installed)
      {
         String filename = FileSystemItem.getNameFromPath(StringUtil.notNull(context.getPath()));
         return SourceDocument.XT_QUARTO_DOCUMENT.equals(context.getExtendedFileType()) ||
                filename.equals("_quarto.yml") ||
                filename.equals("_quarto.yaml");
      }
      else
      {
         return false;
      }  
   }

   @Override
   public void getCompletions(YamlCompletionParams params,
                              CommandWithArg<JsObject> ready)
   {
      
      QuartoEditorToolsYaml.load(() -> {
         try
         {
            QuartoEditorToolsYaml.getCompletions(params).then(
                  new ThenOnFulfilledCallbackFn<JsObject,JsObject>() {
               
               @Override
               public IThenable<JsObject> onInvoke(JsObject result)
               {
                  ready.execute(result);
                  return null;
               }
            },
            new ThenOnRejectedCallbackFn<JsObject>() {
               
               @Override
               public IThenable<JsObject> onInvoke(Object error)
               {
                  Debug.log(error.toString());
                  ready.execute(null);
                  return null;
               }
            });
         }
         catch(Exception ex)
         {
            Debug.logException(ex);
            ready.execute(null);
         }
         
      });   
   } 
   
   private QuartoConfig config_;
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
class QuartoEditorToolsYaml
{
   @JsOverlay
   public static void load(ExternalJavaScriptLoader.Callback onLoaded) 
   {    
      yamlToolsLoader.addCallback(onLoaded);
   }
   
   @JsOverlay
   public static boolean isLoaded() 
   {
      return yamlToolsLoader.isLoaded();
   }
   
   public static native Promise<JsObject> getCompletions(YamlCompletionParams params);
 
   @JsOverlay
   private static final ExternalJavaScriptLoader yamlToolsLoader =
     new ExternalJavaScriptLoader("quarto/resources/editor/tools/yaml/yaml.js");
}
