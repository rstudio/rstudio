/*
 * SourceVimCommands.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
   public final native void initialize(Source source, ShortcutViewer viewer) /*-{
      
      var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
      
      // Save current document
      Vim.defineEx("write", "w", $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::saveActiveSourceDoc()();
      }));
      
      // Select tab by index
      for (var i = 1; i < 100; i++) {
         (function(i) {
            Vim.defineEx("b" + i, "b" + i, $entry(function(cm, params) {
               source.@org.rstudio.studio.client.workbench.views.source.Source::vimSetTabIndex(I)(i - 1);
            }));
         })(i);
      }
      
      // Select next tab
      Vim.defineAction("selectNextTab", $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::nextTabWithWrap()();
      }));
      
      Vim.mapCommand({
         keys: "gt",
         type: "action",
         action: "selectNextTab",
         isEdit: false,
         context: "normal"
      });
      
      Vim.mapCommand({
         keys: "]b",
         type: "action",
         action: "selectNextTab",
         isEdit: false,
         context: "normal"
      });

      // Select previous tab
      Vim.defineAction("selectPreviousTab", $entry(function(cm, args, vim) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::prevTabWithWrap()();
      }));
      
      Vim.mapCommand({
         keys: "gT",
         type: "action",
         action: "selectPreviousTab",
         isEdit: false,
         context: "normal"
      });
      
      Vim.mapCommand({
         keys: "[b",
         type: "action",
         action: "selectPreviousTab",
         isEdit: false,
         context: "normal"
      });
      
      // Select next marker
      Vim.defineAction("selectNextMarker", $entry(function(cm, args) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::selectMarkerRelative(I)(1);
      }));
      
      Vim.mapCommand({
         keys: "]q",
         type: "action",
         action: "selectNextMarker",
         isEdit: false,
         context: "normal"
      });
      
      // Select previous marker
      Vim.defineAction("selectPreviousMarker", $entry(function(cm, args) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::selectMarkerRelative(I)(-1);
      }));
      
      Vim.mapCommand({
         keys: "[q",
         type: "action",
         action: "selectPreviousMarker",
         isEdit: false,
         context: "normal"
      });
      
      // Select next buffer
      Vim.defineEx("bnext", "bn", $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::onNextTab()();
      }));
      
      // Select previous buffer
      Vim.defineEx("bprev", "bp", $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::onPreviousTab()();
      }));
      
      // Close document
      var closeDocument = $entry(function(cm, params) {
      
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.Source::closeSourceDoc(Z)(interactive);
      });
       
      Vim.defineEx("bdelete", "bd", closeDocument);
      Vim.defineEx("quit", "q", closeDocument);
      
      // Close other tabs
      Vim.defineEx("only", "on", $entry(function(cm, params) {
         
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
            
         source.@org.rstudio.studio.client.workbench.views.source.Source::onCloseOtherSourceDocs()();
         
      }));
      
      // Close all tabs
      Vim.defineEx("qall", "qa", $entry(function(cm, params) {
         var interactive = true;
         if (params.argString && params.argString === "!")
            interactive = false;
         
         source.@org.rstudio.studio.client.workbench.views.source.Source::closeAllTabs(Z)(interactive);
      }));
      
      // Create new document
      var createNewDocument = $entry(function(cm, params) {
         
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
      
      Vim.defineEx("badd", "bad", createNewDocument);
      Vim.defineEx("edit", "e", createNewDocument);
      Vim.defineEx("tabedit", "tabe", createNewDocument);
   
      // Save and close active tab
      Vim.defineEx("wq", "wq", $entry(function(cm, params) {
         source.@org.rstudio.studio.client.workbench.views.source.Source::saveAndCloseActiveSourceDoc()();
      }));
      
      // Read a file and paste contents at cursor position
      Vim.defineEx("read", "r", $entry(function(cm, params) {
         if (params.args && params.args.length === 1) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::pasteFileContentsAtCursor(Ljava/lang/String;)(params.args[0]);
         }
      }));
      
      // Run an R script
      Vim.defineEx("Rscript", "R", $entry(function(cm, params) {
         if (params.args) {
            source.@org.rstudio.studio.client.workbench.views.source.Source::pasteRCodeExecutionResult(Ljava/lang/String;)(params.argString);
         }
      }));
   
     // Reflow text
     var reflowText = $entry(function(cm, args, vim) {
        source.@org.rstudio.studio.client.workbench.views.source.Source::reflowText()();
        if (vim.visualMode)
           Vim.exitVisualMode(cm, false);
     });
     
     Vim.defineAction("reflowText", reflowText);
     Vim.mapCommand({
        keys: "gq",
        type: "action",
        action: "reflowText",
        isEdit: true,
        context: "visual"
     });
     Vim.mapCommand({
        keys: "gqq",
        type: "action",
        action: "reflowText",
        isEdit: true,
        context: "normal"
     });
   
     // Re-indent
     var reindent = $entry(function(cm, args, vim) {
        source.@org.rstudio.studio.client.workbench.views.source.Source::reindent()();
        if (vim.visualMode)
          Vim.exitVisualMode(cm, false);
     });
      
      Vim.defineAction("reindent", reindent);
      
      Vim.mapCommand({
         keys: "==",
         type: "action",
         action: "reindent",
         isEdit: true,
         context: "normal"
      });
      
      Vim.mapCommand({
         keys: "=",
         type: "action",
         action: "reindent",
         isEdit: true,
         context: "visual"
      });
      
      // Show help at cursor position
      var showHelpAtCursor = $entry(function(cm, args, vim) {
        source.@org.rstudio.studio.client.workbench.views.source.Source::showHelpAtCursor()();
     });
     
     Vim.defineAction("showHelpAtCursor", showHelpAtCursor);
     Vim.mapCommand({
        keys: "K",
        type: "action",
        action: "showHelpAtCursor",
        isEdit: false,
        context: "normal"
     });

     // Show vim help
     Vim.defineEx("help", "help", $entry(function(cm, params) {
        viewer.@org.rstudio.core.client.command.ShortcutViewer::showVimKeyboardShortcuts()();
     }));
      
     // Expand / shrink selection
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
     Vim.mapCommand({
        keys: "v",
        type: "motion",
        motion: "expandSelection",
        context: "visual"
     });
     
     var shrinkCallback = $entry(function(cm, origHead, motionArgs, vim) {
        vim.sel = toVimSelection(cm.ace.$shrinkSelection());
     });
     
     Vim.defineMotion("shrinkSelection", shrinkCallback);
     Vim.mapCommand({
        keys: "V",
        type: "motion",
        motion: "shrinkSelection",
        context: "visual"
     });
   
   }-*/;
}
