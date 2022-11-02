/*
 * YamlEditorToolsProviderQuarto.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import elemental2.promise.IThenable;
import elemental2.promise.Promise;
import elemental2.promise.IThenable.ThenOnFulfilledCallbackFn;
import elemental2.promise.IThenable.ThenOnRejectedCallbackFn;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


public class YamlEditorToolsProviderQuarto implements YamlEditorToolsProvider
{

   public YamlEditorToolsProviderQuarto()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(Session session, ApplicationServerOperations server)
   {
      config_ = session.getSessionInfo().getQuartoConfig();
      server_ = server;
   }
   
   @Override
   public boolean isActive(String path, String extendedType)
   {
      if (config_.enabled)
      {
         String filename = FileSystemItem.getNameFromPath(StringUtil.notNull(path));
         return SourceDocument.XT_QUARTO_DOCUMENT.equals(extendedType) ||
                isQuartoProjectYaml(filename) ||  isQuartoExtensionYaml(filename) ||
                isQuartoMetadataYaml(path);
      }
      else
      {
         return false;
      }  
   }
   
   private boolean isQuartoProjectYaml(String filename)
   {
      return filename.matches("^_quarto(-.*?)?\\.(yml|yaml)$");
   }
   
   private boolean isQuartoExtensionYaml(String filename)
   {
      return filename.equals("_extension.yml") ||
             filename.equals("_extension.yaml");
   }
   
   private boolean isQuartoMetadataYaml(String path)
   {
      if (QuartoHelper.isWithinQuartoProjectDir(path, config_))
      {
         String filename = FileSystemItem.getNameFromPath(StringUtil.notNull(path));
         return filename.equals("_metadata.yml") ||
                filename.equals("_metadata.yaml");
      }
      else
      {
         return false;
      }
   }

   @Override
   public void getCompletions(YamlEditorContext params,
                              CommandWithArg<JsObject> ready)
   {
      withQuartoTools(() -> {
         handleToolResult(QuartoYamlEditorTools.getCompletions(params, scriptUrl()), ready);
      }, ready);
   } 
   
   @Override
   public void getLint(YamlEditorContext params,  CommandWithArg<JsObject> ready)
   {
      withQuartoTools(() -> {
         handleToolResult(QuartoYamlEditorTools.getLint(params, scriptUrl()), ready);
      }, ready);
   } 
   
   private String scriptUrl()
   {
      return server_.getApplicationURL(QuartoYamlEditorTools.SCRIPT_PATH);
   }
   
   private void withQuartoTools(Command command, CommandWithArg<JsObject> ready)
   {
      QuartoYamlEditorTools.load(() -> {
         try
         {
            command.execute();
         }
         catch(Exception ex)
         {
            Debug.logException(ex);
            ready.execute(null);
         }
      });   
   }
   
   private void handleToolResult(Promise<JsObject> result, CommandWithArg<JsObject> ready)
   {
      result.then(new ThenOnFulfilledCallbackFn<JsObject,JsObject>() {
               
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
   
   
   private QuartoConfig config_;
   private ApplicationServerOperations server_;
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
class QuartoYamlEditorTools
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
   
   public static native Promise<JsObject> getCompletions(YamlEditorContext context, String scriptUrl);
   
   public static native Promise<JsObject> getLint(YamlEditorContext context, String scriptUrl);
   
   @JsOverlay
   public final static String SCRIPT_PATH = "quarto/resources/editor/tools/yaml/yaml.js";
 
   @JsOverlay
   private static final ExternalJavaScriptLoader yamlToolsLoader =
     new ExternalJavaScriptLoader(SCRIPT_PATH);
   
   
}
