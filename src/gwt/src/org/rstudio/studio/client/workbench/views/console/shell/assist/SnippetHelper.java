package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.JsArrayUtil;
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
      editor_ = editor;
      native_ = editor.getWidget().getEditor();
      manager_ = getSnippetManager();
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
         "\t#ifndef ${1}",
         "\t#define ${1}",
         "",
         "\t${0}",
         "",
         "\t#endif // ${1}",
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
         "snippet sg",
         "\tsetGeneric(\"${1:generic}\", function(${2:x, ...}) {",
         "\t\tstandardGeneric(\"${1:generic}\")",
         "\t})",
         "snippet sm",
         "\tsetMethod(${1:f}, signature(${2:name} = \"${3:type}\"}), function(${2:name}, ...) {",
         "\t\t${0}",
         "\t})",
         "snippet sc",
         "\tsetClass(\"${1:Class}\", slots = c(${2:name = \"type\"}))"
      ].join("\n");
      
      var parsed = manager.parseSnippetFile(snippetText);
      manager.register(parsed, "r");
      
   }-*/;
   
   public ArrayList<String> getAvailableSnippets()
   {
      ensureSnippetsLoaded();
      
      String mode = editor_.getLanguageMode(
            editor_.getCursorPosition());
      
      if (mode == null)
         mode = "r";
      
      return JsArrayUtil.fromJsArrayString(
            getAvailableSnippetsImpl(manager_, mode));
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
      applySnippetImpl(snippetName, manager_, editor_.getWidget().getEditor());
   }
   
   public final native void applySnippetImpl(
         String snippetName,
         SnippetManager manager,
         AceEditorNative editor) /*-{
      var content = manager.getSnippetByName(snippetName, editor).content;
      manager.insertSnippet(editor, content);
   }-*/;
   
   private static final native JsArrayString getAvailableSnippetsImpl(
         SnippetManager manager,
         String mode) /*-{
      var snippetsForMode = manager.snippetNameMap[mode];
      if (snippetsForMode)
         return Object.keys(snippetsForMode);
      return [];
   }-*/;

   private final AceEditor editor_;
   private final AceEditorNative native_;
   private final SnippetManager manager_;
   
   private static boolean customCppSnippetsLoaded_;
   private static boolean customRSnippetsLoaded_;
}
