/*
 * Source.java
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
package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.command.ShortcutViewer;

public class SourceVimCommands
{
   public final native void save(Source source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("write", "w",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::saveActiveSourceDoc()();
         })
      );
   }-*/;
   
   public native final void selectNextTab(Source source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bnext", "bn",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::onNextTab()();
         })
      );
   }-*/;
   
   public native final void selectPreviousTab(Source source) /*-{
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bprev", "bp",
         $entry(function(cm, params) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::onPreviousTab()();
         })
      );
   }-*/;
   
   public native final void closeActiveTab(Source source) /*-{
      var callback = $entry(function(cm, params) {
      
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.Source::closeSourceDoc(Z)(interactive);
      });
       
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("bdelete", "bd", callback);
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("quit", "q", callback);
   }-*/;
   
   public native final void closeAllTabs(Source source) /*-{
      var callback = $entry(function(cm, params) {
      
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.Source::closeAllTabs(Z)(interactive);
      });
       
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("qall", "qa", callback);
   }-*/;
   
   public native final void createNewDocument(Source source) /*-{
   
      var callback = $entry(function(cm, params) {
         
         // Handle 'e!'
         if (params.argString && params.argString === "!")
            source.@org.rstudio.studio.client.workbench.views.source.Source::revertActiveDocument()();
            
         // Handle other editing targets
         else if (params.args) {
            if (params.args.length === 1) {
               source.@org.rstudio.studio.client.workbench.views.source.Source::editFile(Ljava/lang/String;)(params.args[0]);
            }
            // TODO: on error?
         } else {
            source.@org.rstudio.studio.client.workbench.views.source.Source::onNewSourceDoc()();
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("badd", "bad", callback);
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("edit", "e", callback);
      
   }-*/;
   
   public native final void saveAndCloseActiveTab(Source source) /*-{
   
      var callback = $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::saveAndCloseActiveSourceDoc()();
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("wq", "wq", callback);
      
   }-*/;
   
   public native final void readFile(Source source, String encoding) /*-{
   
      var callback = $entry(function(cm, params) {
         if (params.args && params.args.length === 1) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::pasteFileContentsAtCursor(Ljava/lang/String;Ljava/lang/String;)(params.args[0], encoding);
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("read", "r", callback);
   
   }-*/;
   
   public native final void runRScript(Source source) /*-{
      
      var callback = $entry(function(cm, params) {
         if (params.args) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::pasteRCodeExecutionResult(Ljava/lang/String;)(params.argString);
         }
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("Rscript", "R", callback);
   
   }-*/;
   
   public native final void showVimHelp(ShortcutViewer viewer) /*-{

      var callback = $entry(function(cm, params) {
         viewer.@org.rstudio.core.client.command.ShortcutViewer::showVimKeyboardShortcuts()();
      });
      
      $wnd.require("ace/keyboard/vim").CodeMirror.Vim.defineEx("help", "help", callback);
   }-*/;
}
