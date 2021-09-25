/*
 * QuartoEditorToolsYaml.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import org.rstudio.core.client.ExternalJavaScriptLoader;

import elemental2.core.JsObject;
import elemental2.promise.Promise;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "QuartoYamlEditorTools")
class YamlEditorToolsQuarto
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
   
   public static native Promise<JsObject> getCompletions(YamlEditorContext context);
   
   public static native Promise<JsObject> getDiagnostics(YamlEditorContext context);
 
   @JsOverlay
   private static final ExternalJavaScriptLoader yamlToolsLoader =
     new ExternalJavaScriptLoader("quarto/resources/editor/tools/yaml/yaml.js");
}