/*
 * RFileType.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;

import java.util.HashSet;

public class RFileType extends TextFileType
{
   RFileType(String id,
             String label,
             EditorLanguage editorLanguage,
             String defaultExtension,
             ImageResource defaultIcon)
   {
      super(id,
            label,
            editorLanguage,
            defaultExtension,
            defaultIcon,
            false, true, true, true, true, false, 
            false, false, false, true, false, true, false);
   }

   @Override
   public boolean getWordWrap()
   {
      return RStudioGinjector.INSTANCE.getUserPrefs().softWrapRFiles().getValue();
   }

   @Override
   public boolean canCompileNotebook()
   {
      return true;
   }

   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      result.add(commands.jumpTo());
      result.add(commands.jumpToMatching());
      result.add(commands.goToHelp());
      result.add(commands.goToDefinition());
      result.add(commands.insertSection());
      result.add(commands.codeCompletion());
      result.add(commands.debugBreakpoint());
      result.add(commands.insertRoxygenSkeleton());
      return result;
   }

   @Override
   public TokenPredicate getSpellCheckTokenPredicate()
   {
      return (token, row, column) ->
      {
         if (reNospellType_.match(token.getType(), 0) != null) {
            return false;
         }

         return reCommentType_.match(token.getType(), 0) != null &&
            reKeywordType_.match(token.getType(), 0) == null &&
            reIdentifierType_.match(token.getType(), 0) == null;
      };
   }
}
