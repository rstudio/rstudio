/*
 * TextEditingTargetPrefsHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.model.ProjectConfig;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.event.shared.HandlerRegistration;

public class TextEditingTargetPrefsHelper
{
   public enum PrefsSet
   {
      Embedded,  // The prefs to be used for embedded targets
      Full       // The prefs to be used for full targets
   }

   public static interface PrefsContext
   {
      FileSystemItem getActiveFile();
   }

   public static void registerPrefs(
                     ArrayList<HandlerRegistration> releaseOnDismiss,
                     UserPrefs prefs,
                     ProjectConfig projectConfig,
                     DocDisplay docDisplay,
                     final SourceDocument sourceDoc)
   {
      registerPrefs(releaseOnDismiss,
                    prefs,
                    projectConfig,
                    docDisplay,
                    () -> {
                        String path = sourceDoc.getPath();
                        if (path != null)
                           return FileSystemItem.createFile(path);
                        else
                           return null;
                     },
                    PrefsSet.Full);
   }

   public static void registerPrefs(
                     ArrayList<HandlerRegistration> releaseOnDismiss,
                     UserPrefs prefs,
                     final ProjectConfig projectConfig,
                     final DocDisplay docDisplay,
                     final PrefsContext context,
                     PrefsSet prefsSet)
   {
      releaseOnDismiss.add(prefs.highlightSelectedWord().bind(
            (arg) ->
            {
               docDisplay.setHighlightSelectedWord(arg);
            }));
      releaseOnDismiss.add(prefs.useSpacesForTab().bind(
            (arg) ->
            {
               if (TextEditingTarget.shouldEnforceHardTabs(context.getActiveFile()))
               {
                  docDisplay.setUseSoftTabs(false);
               }
               else
               {
                  if (projectConfig == null)
                     docDisplay.setUseSoftTabs(arg);
               }
             }));
      releaseOnDismiss.add(prefs.numSpacesForTab().bind(
            (arg) ->
            {
               if (projectConfig == null)
                 docDisplay.setTabSize(arg);
            }));
      releaseOnDismiss.add(prefs.autoDetectIndentation().bind(
            (arg) ->
            {
               if (projectConfig == null)
                 docDisplay.autoDetectIndentation(arg);
            }));
      releaseOnDismiss.add(prefs.blinkingCursor().bind(
            (arg) ->
            {
               docDisplay.setBlinkingCursor(arg);
            }));
      releaseOnDismiss.add(prefs.marginColumn().bind(
            (arg) ->
            {
               docDisplay.setPrintMarginColumn(arg);
            }));
      releaseOnDismiss.add(prefs.showInvisibles().bind(
            (arg) ->
            {
               docDisplay.setShowInvisibles(arg);
            }));
      releaseOnDismiss.add(prefs.showIndentGuides().bind(
            (arg) ->
            {
               docDisplay.setShowIndentGuides(arg);
            }));
      releaseOnDismiss.add(prefs.scrollPastEndOfDocument().bind(
            (arg) ->
            {
               docDisplay.setScrollPastEndOfDocument(arg);
            }));
      releaseOnDismiss.add(prefs.highlightRFunctionCalls().bind(
            (arg) ->
            {
               docDisplay.setHighlightRFunctionCalls(arg);
            }));
      releaseOnDismiss.add(prefs.rainbowParentheses().bind(
            (arg) ->
            {
               docDisplay.setRainbowParentheses(arg);
            }));
      releaseOnDismiss.add(prefs.codeCompletionOther().bind(
            (arg) ->
            {
               docDisplay.syncCompletionPrefs();
            }));
      releaseOnDismiss.add(prefs.codeCompletionCharacters().bind(
            (arg) ->
            {
               docDisplay.syncCompletionPrefs();
            }));
      releaseOnDismiss.add(prefs.codeCompletionDelay().bind(
            (arg) ->
            {
               docDisplay.syncCompletionPrefs();
            }));
      releaseOnDismiss.add(prefs.enableSnippets().bind(
            (arg) ->
            {
               docDisplay.syncCompletionPrefs();
            }));
      releaseOnDismiss.add(prefs.showDiagnosticsOther().bind(
            (arg) ->
            {
               docDisplay.syncDiagnosticsPrefs();
            }));
      releaseOnDismiss.add(prefs.diagnosticsOnSave().bind(
            (arg) ->
            {
              docDisplay.syncDiagnosticsPrefs();
            }));
      releaseOnDismiss.add(prefs.backgroundDiagnosticsDelayMs().bind(
            (arg) ->
            {
               docDisplay.syncDiagnosticsPrefs();
            }));
      releaseOnDismiss.add(prefs.showInlineToolbarForRCodeChunks().bind(
            (arg) ->
            {
               docDisplay.forceImmediateRender();
            }));
      releaseOnDismiss.add(prefs.foldStyle().bind(
            (arg) ->
            {
               docDisplay.setFoldStyle(FoldStyle.fromPref(arg));
            }));
      releaseOnDismiss.add(prefs.surroundSelection().bind(
            (arg) ->
            {
               docDisplay.setSurroundSelectionPref(arg);
            }));
      releaseOnDismiss.add(prefs.enableTextDrag().bind(
            (arg) ->
            {
               docDisplay.setDragEnabled(arg);
            }));

      // Full editors get additional prefs (we don't use these in embedded editors)
      if (prefsSet == PrefsSet.Full)
      {
         releaseOnDismiss.add(prefs.showMargin().bind(
               (arg) ->
               {
                  docDisplay.setShowPrintMargin(arg);
               }));
         releaseOnDismiss.add(prefs.showLineNumbers().bind(
               (arg) ->
               {
                  docDisplay.setShowLineNumbers(arg);
               }));
         releaseOnDismiss.add(prefs.highlightSelectedLine().bind(
               (arg) ->
               {
                  docDisplay.setHighlightSelectedLine(arg);
               }));
         releaseOnDismiss.add(prefs.editorKeybindings().bind(
               (arg) ->
               {
                  docDisplay.setUseVimMode(arg == UserPrefs.EDITOR_KEYBINDINGS_VIM);
               }));
         releaseOnDismiss.add(prefs.editorKeybindings().bind(
               (arg) ->
               {
                  docDisplay.setUseEmacsKeybindings(arg == UserPrefs.EDITOR_KEYBINDINGS_EMACS);
               }));
      }
      
      // Embedded mode specific prefs
      if (prefsSet == PrefsSet.Embedded)
      {
         releaseOnDismiss.add(prefs.visualMarkdownEditingShowMargin().bind(
               (arg) ->
               {
                  docDisplay.setShowPrintMargin(arg);
               }));
      }
   }
}
