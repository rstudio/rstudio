/*
 * SourceVimCommands.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.command.ShortcutViewer;

import com.google.gwt.user.client.Command;

public class SourceVimCommands
{
   public final native void save(SourceColumnManager source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("write", "w",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::saveActiveSourceDoc()();
         })
      );
   }-*/;
   
   public native final void selectTabIndex(SourceColumnManager source) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      for (var i = 1; i <= 100; i++) {
         (function(i) {
            Vim.defineEx("b" + i, "b" + i, $entry(function(cm, params) {
               source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::vimSetTabIndex(I)(i - 1);
            }));
         })(i);
      }
      
      var nextTab = $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::nextTabWithWrap()();
      });
     
      Vim.defineAction("selectNextTab", nextTab);
      Vim._mapCommand({
         keys: "gt",
         type: "action",
         action: "selectNextTab",
         isEdit: false,
         context: "normal"
      });

      var prevTab = $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::prevTabWithWrap()();
      });
     
      Vim.defineAction("selectPreviousTab", prevTab);
      Vim._mapCommand({
         keys: "gT",
         type: "action",
         action: "selectPreviousTab",
         isEdit: false,
         context: "normal"
      });
   }-*/;
   
   public native final void selectNextTab(SourceColumnManager source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bnext", "bn",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::onNextTab()();
         })
      );
   }-*/;
   
   public native final void selectPreviousTab(SourceColumnManager source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bprev", "bp",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::onPreviousTab()();
         })
      );
   }-*/;
   
   public native final void closeActiveTab(SourceColumnManager source) /*-{
      var callback = $entry(function(cm, params) {
      
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::closeSourceDoc(Z)(interactive);
      });
       
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bdelete", "bd", callback);
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("quit", "q", callback);
   }-*/;
   
   public native final void closeAllTabs(SourceColumnManager source, Command onCompleted) /*-{
      var callback = $entry(function(cm, params) {
      
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::closeAllTabs(ZZLcom/google/gwt/user/client/Command;)(interactive, false, onCompleted);
      });
       
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("qall", "qa", callback);
   }-*/;
   
   public native final void createNewDocument(SourceColumnManager source) /*-{
   
      var callback = $entry(function(cm, params) {
         
         // Handle 'e!'
         if (params.argString && params.argString === "!")
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::revertActiveDocument()();
            
         // Handle other editing targets
         else if (params.args) {
            if (params.args.length === 1) {
               source.getColumnManager()().@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::vimEditFile(Ljava/lang/String;)(params.args[0]);
            }
            // TODO: on error?
         } else {
            source.@org.rstudio.studio.client.workbench.views.source.Source::onNewSourceDoc()();
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("badd", "bad", callback);
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("edit", "e", callback);
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("tabedit", "tabe", callback);
      
   }-*/;
   
   public native final void saveAndCloseActiveTab(SourceColumnManager source) /*-{
   
      var callback = $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::saveAndCloseActiveSourceDoc()();
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("wq", "wq", callback);
      
   }-*/;
   
   public native final void readFile(SourceColumnManager source, String encoding) /*-{
   
      var callback = $entry(function(cm, params) {
         if (params.args && params.args.length === 1) {
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::pasteFileContentsAtCursor(Ljava/lang/String;Ljava/lang/String;)(params.args[0], encoding);
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("read", "r", callback);
   
   }-*/;
   
   public native final void runRScript(SourceColumnManager source) /*-{
      
      var callback = $entry(function(cm, params) {
         if (params.args) {
            source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::pasteRCodeExecutionResult(Ljava/lang/String;)(params
                .argString);
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("Rscript", "R", callback);
   
   }-*/;
   
   public native final void reflowText(SourceColumnManager source) /*-{
   
     var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
     
     var callback = $entry(function(cm, args, vim) {
        source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::reflowText()();
        if (vim.visualMode)
           Vim.exitVisualMode(cm, false);
     });
     
     Vim.defineAction("reflowText", callback);
     Vim._mapCommand({
        keys: "gq",
        type: "action",
        action: "reflowText",
        isEdit: true,
        context: "visual"
     });
     Vim._mapCommand({
        keys: "gqq",
        type: "action",
        action: "reflowText",
        isEdit: true,
        context: "normal"
     });
   }-*/;
   
   public native final void reindent(SourceColumnManager source) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      var callback = $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::reindent()();
         if (vim.visualMode)
            Vim.exitVisualMode(cm, false);
      });
      
      Vim.defineAction("reindent", callback);
      
      Vim._mapCommand({
         keys: "==",
         type: "action",
         action: "reindent",
         isEdit: true,
         context: "normal"
      });
      
      Vim._mapCommand({
         keys: "=",
         type: "action",
         action: "reindent",
         isEdit: true,
         context: "visual"
      });
      
   }-*/;
   
   public native final void showHelpAtCursor(SourceColumnManager source) /*-{
     var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
     
     var callback = $entry(function(cm, args, vim) {
        source.@org.rstudio.studio.client.workbench.views.source.SourceColumnManager::showHelpAtCursor()();
     });
     
     Vim.defineAction("showHelpAtCursor", callback);
     Vim._mapCommand({
        keys: "K",
        type: "action",
        action: "showHelpAtCursor",
        isEdit: false,
        context: "normal"
     });
   }-*/;

   public native final void showVimHelp(ShortcutViewer viewer) /*-{

      var callback = $entry(function(cm, params) {
         viewer.@org.rstudio.core.client.command.ShortcutViewer::showVimKeyboardShortcuts()();
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("help", "help", callback);
   }-*/;
   
   public native final void expandShrinkSelection(SourceColumnManager source) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      
      function toVimSelection(range) {
         return {
            anchor: {
               line: range.start.row, ch: range.start.column
            },
            head: {
               line: range.end.row, ch: range.end.column - 1
            }
         };
      }
      
      var expandCallback = $entry(function(cm, origHead, motionArgs, vim) {
         vim.sel = toVimSelection(cm.ace.$expandSelection());
      });
      
      Vim.defineMotion("expandSelection", expandCallback);
      Vim._mapCommand({
         keys: "v",
         type: "motion",
         motion: "expandSelection",
         context: "visual"
      });
      
      var shrinkCallback = $entry(function(cm, origHead, motionArgs, vim) {
         vim.sel = toVimSelection(cm.ace.$shrinkSelection());
      });
      
      Vim.defineMotion("shrinkSelection", shrinkCallback);
      Vim._mapCommand({
         keys: "V",
         type: "motion",
         motion: "shrinkSelection",
         context: "visual"
      });
   
   }-*/;
   
   public native final void openNextFile(SourceColumnManager source) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      var callback = $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::onOpenNextFileOnFilesystem()();
      });
      
      Vim.defineAction("openNextFile", callback);
      Vim._mapCommand({
         keys: "]f",
         type: "action",
         action: "openNextFile",
         context: "normal"
      });
      
      Vim._mapCommand({
         keys: "]f",
         type: "action",
         action: "openNextFile",
         context: "visual"
      });
      
   }-*/;
   
   public native final void openPreviousFile(SourceColumnManager source) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      var callback = $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::onOpenPreviousFileOnFilesystem()();
      });
      
      Vim.defineAction("openPreviousFile", callback);
      Vim._mapCommand({
         keys: "[f",
         type: "action",
         action: "openPreviousFile",
         context: "normal"
      });
      
      Vim._mapCommand({
         keys: "[f",
         type: "action",
         action: "openPreviousFile",
         context: "visual"
      });
      
   }-*/;
   
   public native final void addStarRegister() /*-{
      
      var SystemClipboardRegister = function(text, linewise, blockwise) {
         this.clear();
         this.keyBuffer = [text || ''];
         this.insertModeChanges = [];
         this.searchQueries = [];
         this.linewise = !!linewise;
         this.blockwise = !!blockwise;
      }
      
      // TODO: Reimplement this and read/write
      // from the system clipboard using appropriate
      // callbacks.
      SystemClipboardRegister.prototype = {
         
         setText: function(text, linewise, blockwise) {
            this.keyBuffer = [text || ''];
            this.linewise = !!linewise;
            this.blockwise = !!blockwise;
         },
         
         pushText: function(text, linewise) {
            this.keyBuffer.push(text);
            if (linewise) {
               if (!this.linewise) {
                  this.keyBuffer.push('\n');
               }
               this.linewise = true;
            }
            this.keyBuffer.push(text);
         },
         
         clear: function() {
            this.keyBuffer = [];
            this.insertModeChanges = [];
            this.searchQueries = [];
            this.linewise = false;
         },
         
         toString: function() {
            return this.keyBuffer.join('');
         }
         
      };
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      var controller = Vim.getRegisterController();
      // controller.registers['*'] = new SystemClipboardRegister();
   
   }-*/;
}
