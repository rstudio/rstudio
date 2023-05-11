/*
 * TextEditingTargetQuartoHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;


import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.inject.Inject;

public class TextEditingTargetQuartoHelper
{
   public TextEditingTargetQuartoHelper(EditingTarget editingTarget, DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      editingTarget_ = editingTarget;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(Commands commands)
   {
      commands_ = commands;
   }
   
   
   public void manageCommands()
   {
      boolean isQuartoDoc = SourceDocument.XT_QUARTO_DOCUMENT
                                .equals(editingTarget_.getExtendedFileType());
      boolean hasQuartoExt = docDisplay_.getFileType().isQuartoMarkdown();
      
      // enable quarto render for quarto docs
      commands_.quartoRenderDocument().setVisible(isQuartoDoc);
      
      // disable traditional rmd knit commands for quarto docs
      commands_.previewHTML().setVisible(!isQuartoDoc && docDisplay_.getFileType().canPreviewHTML());
      commands_.knitDocument().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      commands_.knitWithParameters().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      commands_.clearKnitrCache().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      
      // disable some rmd file specific stuff for quarto ext
      commands_.executeSetupChunk().setVisible(!hasQuartoExt && docDisplay_.getFileType().canKnitToHTML());
      
   }
   
   public static boolean continueSpecialCommentOnNewline(DocDisplay docDisplay, NativeEvent event)
   {
      // don't do anything if we have a completion popup showing
      if (docDisplay.isPopupVisible())
         return false;
      
      // only handle plain Enter insertions
      if (event.getKeyCode() != KeyCodes.KEY_ENTER)
         return false;
      
      int modifier = KeyboardShortcut.getModifierValue(event);
      if (modifier != KeyboardShortcut.NONE)
         return false;
      
      String line = docDisplay.getCurrentLineUpToCursor();

      // validate that this line begins with a comment character
      // (necessary to check token type for e.g. Markdown documents)
      // https://github.com/rstudio/rstudio/issues/6421
      //
      // note that we don't check all tokens here since we provide
      // special token styling within some comments (e.g. roxygen)
      JsArray<Token> tokens =
            docDisplay.getTokens(docDisplay.getCursorPosition().getRow());
               
      for (int i = 0, n = tokens.length(); i < n; i++)
      {
         Token token = tokens.get(i);

         // skip initial whitespace tokens if any
         String value = token.getValue();
         if (value.trim().isEmpty())
            continue;

         // check that we have a comment
         if (token.hasType("comment"))
            break;
         
         // the token isn't a comment; we shouldn't take action here
         return false;
      }
      
      // if this is an R Markdown chunk metadata comment, and this
      // line is blank other than the comment prefix, remove that
      // prefix and insert a newline (terminating the block)
      {
         Pattern pattern = Pattern.create("^\\s*#\\s*[|]\\s*$", "");
         Match match = pattern.match(line, 0);
         if (match != null)
         {
            Position cursorPos = docDisplay.getCursorPosition();
            Range range = Range.create(
                  cursorPos.getRow(), 0,
                  cursorPos.getRow() + 1, 0);
            
            event.stopPropagation();
            event.preventDefault();
            docDisplay.replaceRange(range, "\n\n");
            docDisplay.moveCursorBackward();
            docDisplay.ensureCursorVisible();
            return true;
         }
      }
      
      // NOTE: we are generous with our pattern definition here
      // as we've already validated this is a comment token above
      Pattern pattern = Pattern.create("^(\\s*(?:#+|%+|//+)['*+>|]\\s*)");
      Match match = pattern.match(line, 0);
      if (match == null)
         return false;
      
      event.preventDefault();
      event.stopPropagation();
      docDisplay.insertCode("\n" + match.getGroup(1));
      docDisplay.ensureCursorVisible();
      
      return true;
   }
   
  
   
   private Commands commands_;
   private DocDisplay docDisplay_;
   private EditingTarget editingTarget_;
}
