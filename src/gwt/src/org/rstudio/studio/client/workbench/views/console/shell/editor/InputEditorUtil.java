/*
 * InputEditorUtil.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.editor;

import com.google.gwt.user.client.Command;

public class InputEditorUtil
{
   public static void yankAfterCursor(final InputEditorDisplay editor,
                                      final boolean saveValue)
   {
      InputEditorSelection selection = editor.getSelection() ;
      if (selection != null)
      {
         selection = selection.extendToLineEnd();

         editor.beginSetSelection(selection, new Command()
         {
            public void execute()
            {
               editor.setFocus(true) ;
               String yanked = editor.replaceSelection("", true);
               if (saveValue)
                  lastYanked = yanked;
            }
         });
      }
   }

   public static void yankBeforeCursor(final InputEditorDisplay editor,
                                       final boolean saveValue)
   {
      InputEditorSelection selection = editor.getSelection() ;
      if (selection != null)
      {
         selection = selection.extendToLineStart();

         editor.beginSetSelection(selection, new Command()
         {
            public void execute()
            {
               editor.setFocus(true) ;
               String yanked = editor.replaceSelection("", true);
               if (saveValue)
                  lastYanked = yanked;
            }
         });
      }
   }

   public static void pasteYanked(InputEditorDisplay editor)
   {
      if (lastYanked != null)
      {
         editor.replaceSelection(lastYanked, true);
      }
   }

   public static boolean moveSelectionToLineStart(InputEditorDisplay editor)
   {
      InputEditorSelection selection = editor.getSelection();

      // In Firefox, sometimes PlainTextEditor can't find the selection
      // (see bug 302 - "Command line editing - When a former line is
      // recalled with the up arrow, the CNTL-a command will not bring
      // the cursor to the start of the line until the left-arrow
      // or a mouse click is used to move one or more characters to
      // the left."
      if (selection == null)
         selection = editor.getStart();

      InputEditorPosition lineStart = selection.getStart()
            .movePosition(0, false);
      editor.beginSetSelection(new InputEditorSelection(lineStart), null);
      return true;
   }

   public static boolean moveSelectionToLineEnd(InputEditorDisplay editor)
   {
      InputEditorSelection selection = editor.getSelection();

      // In Firefox, sometimes PlainTextEditor can't find the selection
      // (see bug 302 - "Command line editing - When a former line is
      // recalled with the up arrow, the CNTL-a command will not bring
      // the cursor to the start of the line until the left-arrow
      // or a mouse click is used to move one or more characters to
      // the left."
      if (selection == null)
         selection = editor.getEnd();

      InputEditorPosition lineEnd = selection.getEnd()
            .movePosition(selection.getEnd().getLineLength(), false);
      editor.beginSetSelection(new InputEditorSelection(lineEnd), null);
      return true;
   }

   public static void extendSelectionToLineStart(InputEditorDisplay editor)
   {
      InputEditorSelection selection = editor.getSelection();

      if (selection == null)
         selection = editor.getStart();

      InputEditorPosition lineStart = selection.getStart()
            .movePosition(0, false);
      editor.beginSetSelection(
            new InputEditorSelection(lineStart,
                                     selection.getEnd()), null);
   }

   public static void extendSelectionToLineEnd(InputEditorDisplay editor)
   {
      InputEditorSelection selection = editor.getSelection();

      if (selection == null)
         selection = editor.getEnd();

      InputEditorPosition lineEnd = selection.getEnd()
            .movePosition(selection.getEnd().getLineLength(), false);
      editor.beginSetSelection(
            new InputEditorSelection(selection.getStart(),
                                     lineEnd), null);
   }

   private static String lastYanked;
}
