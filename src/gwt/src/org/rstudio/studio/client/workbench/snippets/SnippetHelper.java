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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.snippets.model.Snippet;
import org.rstudio.studio.client.workbench.snippets.model.SnippetData;
import org.rstudio.studio.client.workbench.snippets.model.SnippetsChangedEvent;
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
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(SnippetServerOperations server)
   {
      server_ = server;
   }
   
   private static final native SnippetManager getSnippetManager() /*-{
      return $wnd.require("ace/snippets").snippetManager;
   }-*/;
   
   public ArrayList<String> getCppSnippets()
   {
      ensureSnippetsLoaded();
      return JsArrayUtil.fromJsArrayString(
            getAvailableSnippetsImpl(manager_, "c_cpp"));
   }
   
   public Snippet getCppSnippet(String name)
   {    
      return getSnippet(manager_, "c_cpp", name);
   }
   
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
      ensureSnippetsLoaded("r", manager_);
   }
   
   private void ensureCppSnippetsLoaded()
   {
      ensureSnippetsLoaded("c_cpp", manager_);
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
      
      // Overwrite the old snippets stored.
      var old = $wnd.require("rstudio/snippets/" + mode);
      if (old != null) {
         old.$snippetText = old.snippetText;
         old.snippetText = snippetText;
      } else {
         old = $wnd.require("ace/snippets/" + mode);
         if (old != null) {
            old.$snippetText = old.snippetText;
            old.snippetText = snippetText;
         }
      }
      
      // Apply new snippets
      manager.register(snippets, mode);
      return null;
      
   }-*/;
   
   public static final JavaScriptException loadSnippetsForMode(
         String mode,
         String snippetText)
   {
      return loadSnippetsForMode(
            mode,
            snippetText,
            getSnippetManager());
   }
   
   private static final native void ensureSnippetsLoaded(
         String mode,
         SnippetManager manager) /*-{

      var snippetsForMode = manager.snippetNameMap[mode];
      if (!snippetsForMode) {
         
         // Try loading our own, local snippets. Loading those snippets will
         // automatically register the snippets as necessary.
         var m = null;
         m = $wnd.require("rstudio/snippets/" + mode);
         if (m !== null)
            return;
            
         // Try loading internal Ace snippets. We need to pull the snippet
         // content out of the appropriate require, then parse and load those
         // snippets.
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
      
      // For snippets that contain code we want to execute in R, we pass the
      // snippet down to the server and then apply the response.
      if (containsExecutableRCode(snippetContent))
      {
         server_.transformSnippet(snippetContent, new ServerRequestCallback<String>()
         {
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
            
            @Override
            public void onResponseReceived(String transformed)
            {
               applySnippetImpl(transformed, manager_, editor_.getWidget().getEditor());
            }
         });
      }
      else
      {
         applySnippetImpl(snippetContent, manager_, editor_.getWidget().getEditor());
      }
   }
   
   private boolean containsExecutableRCode(String snippetContent)
   {
      return RE_R_CODE.test(snippetContent);
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
   
   public static void onSnippetsChanged(SnippetsChangedEvent event)
   {
      SnippetManager manager = getSnippetManager();
      JsArray<SnippetData> data = event.getData();
      for (int i = 0; i < data.length(); i++)
      {
         SnippetData snippetData = data.get(i);
         loadSnippetsForMode(
               snippetData.getMode(),
               snippetData.getContents(),
               manager);
      }
   }
   
   private final AceEditor editor_;
   private final AceEditorNative native_;
   private final SnippetManager manager_;
   private final String path_;
   
   private SnippetServerOperations server_;
   
   private static boolean customCppSnippetsLoaded_;
   private static boolean customRSnippetsLoaded_;
   
   private static final Pattern RE_R_CODE =
         Pattern.create("`[Rr]\\s+[^`]+`", "");
}
