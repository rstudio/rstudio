/*
 * SnippetHelper.java
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

package org.rstudio.studio.client.workbench.snippets;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.workbench.snippets.model.Snippet;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

import java.util.ArrayList;

public class SnippetHelper
{
   static class SnippetManager extends JavaScriptObject
   {
      protected SnippetManager() {}
   }
   
   public SnippetHelper(AceEditor editor)
   {
      this(editor, null);
   }
   
   public SnippetHelper(AceEditor editor, String path)
   {
      editor_ = editor;
      native_ = editor.getWidget().getEditor();
      manager_ = getSnippetManager();
      path_ = path;
   }
   
   private static final native SnippetManager getSnippetManager() /*-{
      return $wnd.require("ace/snippets").snippetManager;
   }-*/;
   
   public ArrayList<String> getCppSnippets()
   {
      ensureSnippetsLoaded();
      ensureCustomCppSnippetsLoaded();
      return JsArrayUtil.fromJsArrayString(
            getAvailableSnippetsImpl(manager_, "c_cpp"));
   }
   
   public Snippet getCppSnippet(String name)
   {    
      return getSnippet(manager_, "c_cpp", name);
   }
   
   private void ensureCustomCppSnippetsLoaded()
   {
      if (!customCppSnippetsLoaded_)
      {
         addCustomCppSnippets(manager_);
         customCppSnippetsLoaded_ = true;
      }
   }
   
   private final native void addCustomCppSnippets(SnippetManager manager)
   /*-{
      var snippetText = [
         "## Header guard",
         "snippet once",
         "\t#ifndef ${1:`HeaderGuardFileName`}",
         "\t#define ${1:`HeaderGuardFileName`}",
         "",
         "\t${0}",
         "",
         "\t#endif // ${1:`HeaderGuardFileName`}",
         "##",
         "## Anonymous namespace",
         "snippet ans",
         "\tnamespace {",
         "\t${0}",
         "\t} // anonymous namespace",
         "##",
         "## Named namespace",
         "snippet ns",
         "\tnamespace ${1} {",
         "\t${0}",
         "\t} // namespace ${1}",
         "##",
         "## class",
         "snippet cls",
         "\tclass ${1} {",
         "\tpublic:",
         "\t\t${2}",
         "\tprivate:",
         "\t\t${3}",
         "\t};",
         "##",
         "## struct",
         "snippet str",
         "\tstruct ${1} {",
         "\t\t${0}",
         "\t};",
         "##",
         "## cerr",
         "snippet cerr",
         "\tstd::cerr << ${1} << std::endl;${0}",
         "##",
         "snippet main",
         "\tint main(int argc, char* argv[]) {",
         "\t\t${0}",
         "\t}",
      ].join("\n");
      
      var parsed = manager.parseSnippetFile(snippetText);
      manager.register(parsed, "c_cpp");
   }-*/;
   
   private void ensureCustomRSnippetsLoaded()
   {
      if (!customRSnippetsLoaded_)
      {
         loadCustomRSnippets(manager_);
         customRSnippetsLoaded_ = true;
      }
   }
   
   private final native void loadCustomRSnippets(SnippetManager manager)
   /*-{
      
      var snippetText = [
         "snippet sserver",
         "\tshinyServer(function(input, output, session) {",
         "\t\t${0}",
         "\t})",
         "snippet dig",
         "\tdevtools::install_github(\"${0}\")",
         "## S4",
         "snippet setG",
         "\tsetGeneric(\"${1:generic}\", function(${2:x, ...}) {",
         "\t\tstandardGeneric(\"${1:generic}\")",
         "\t})",
         "snippet setM",
         "\tsetMethod(\"${1:generic}\", ${2:\"class\"}, function(${3:object}, ...) {",
         "\t\t${0}",
         "\t})",
         "snippet setC",
         "\tsetClass(\"${1:Class}\", slots = c(${2:name = \"type\"}))"
      ].join("\n");
      
      var parsed = manager.parseSnippetFile(snippetText);
      manager.register(parsed, "r");
      
   }-*/;
   
   public ArrayList<String> getAvailableSnippets()
   {
      ensureSnippetsLoaded();
      
      return JsArrayUtil.fromJsArrayString(
            getAvailableSnippetsImpl(manager_, getEditorMode()));
   }
   
   private final void ensureSnippetsLoaded()
   {
      ensureRSnippetsLoaded();
      ensureCppSnippetsLoaded();
   }
   
   private void ensureRSnippetsLoaded()
   {
      ensureAceSnippetsLoaded("r", manager_);
      ensureCustomRSnippetsLoaded();
   }
   
   private void ensureCppSnippetsLoaded()
   {
      ensureAceSnippetsLoaded("c_cpp", manager_);
      ensureCustomCppSnippetsLoaded();
   }
   
   // Parse a snippet file and apply the parsed snippets for
   // mode 'mode'. Returns an associated exception on failure,
   // or 'null' on success.
   public static final native JavaScriptException loadSnippetsForMode(
         String mode,
         String snippetText,
         SnippetManager manager)
   /*-{
      
      // Parse snippets passed through
      var snippets = null;
      try {
         snippets = manager.parseSnippetFile(snippetText)
      } catch (e) {
         return e;
      }
      
      // Clear old snippets associated with this mode
      delete manager.snippetMap[mode];
      delete manager.snippetNameMap[mode];
      
      // Apply new snippets
      manager.register(snippets, mode);
      return null;
      
   }-*/;
   
   private static final native void ensureAceSnippetsLoaded(
         String mode,
         SnippetManager manager) /*-{

      var snippetsForMode = manager.snippetNameMap[mode];
      if (!snippetsForMode) {
         var id = "ace/snippets/" + mode;
         var m = $wnd.require(id);
         if (!m) {
            console.log("Failed load Ace snippets for mode '" + mode + "'");
            return;
         }
         
         if (!manager.files)
            manager.files = {};
            
         manager.files[id] = m;
         if (!m.snippets && m.snippetText)
         m.snippets = manager.parseSnippetFile(m.snippetText);
         manager.register(m.snippets || [], m.scope);
      }
      
   }-*/;
   
   public void applySnippet(String token, String snippetName)
   {
      editor_.expandSelectionLeft(token.length());
      String snippetContent = transformMacros(
            getSnippetContents(snippetName));
      applySnippetImpl(snippetContent, manager_, editor_.getWidget().getEditor());
   }
   
   private String replaceFilename(String snippet)
   {
      String fileName = FilePathUtils.fileNameSansExtension(path_);
      return snippet.replaceAll("`Filename.*`", fileName);
   }
   
   private String replaceHeaderGuard(String snippet)
   {
      // Munge the path a bit
      String path = path_;
      if (path.startsWith("~/"))
         path = path.substring(2);
         
      int instIncludeIdx = path.indexOf("/inst/include/");
      if (instIncludeIdx != -1)
         path = path.substring(instIncludeIdx + 15);
      
      int srcIdx = path.indexOf("/src/");
      if (srcIdx != -1)
         path = path.substring(srcIdx + 6);
      
      path = path.replaceAll("[./]", "_");
      path = path.toUpperCase();
      
      return snippet.replaceAll("`HeaderGuardFileName`", path);
   }
   
   private String transformMacros(String snippet)
   {
      if (path_ != null)
      {
         snippet = replaceFilename(snippet);
         snippet = replaceHeaderGuard(snippet);
      }
      return snippet;
   }
   
   public final native void applySnippetImpl(
         String snippetContent,
         SnippetManager manager,
         AceEditorNative editor) /*-{
      manager.insertSnippet(editor, snippetContent);
   }-*/;
   
   private static final native JsArrayString getAvailableSnippetsImpl(
         SnippetManager manager,
         String mode) /*-{
      var snippetsForMode = manager.snippetNameMap[mode];
      if (snippetsForMode)
         return Object.keys(snippetsForMode);
      return [];
   }-*/;

   private static final native Snippet getSnippet(
         SnippetManager manager,
         String mode,
         String name) /*-{
      var snippetsForMode = manager.snippetNameMap[mode];
      if (snippetsForMode)
         return snippetsForMode[name];
      else
         return null;
   }-*/;
   
   // NOTE: this function assumes you've already called ensureSnippetsLoaded
   // (this is a safe assumption because in order to enumerate snippet names
   // you need to call the ensure* functions)
   public String getSnippetContents(String snippetName)
   {
      return getSnippet(manager_, getEditorMode(), snippetName).getContent();
   }
   
   private String getEditorMode()
   {
      String mode = editor_.getLanguageMode(
            editor_.getCursorPosition());
      
      if (mode == null)
         mode = "r";
      
      return mode.toLowerCase();
   }
   
   private final AceEditor editor_;
   private final AceEditorNative native_;
   private final SnippetManager manager_;
   private final String path_;
   
   private static boolean customCppSnippetsLoaded_;
   private static boolean customRSnippetsLoaded_;
}
