/*
 * JuliaFileType.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.common.filetypes;

import java.util.HashSet;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.shell.ConsoleLanguageTracker;

import com.google.gwt.resources.client.ImageResource;

// Julia scripts, whose code runs in the console's Julia REPL
public class JuliaFileType extends TextFileType
{
   JuliaFileType(
         String id,
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
            WordWrap.DEFAULT, // word wrap
            false, // source on save
            true,  // execute code
            true,  // execute all code
            true,  // execute to current line
            false, // preview HTML
            false, // knit to HTML
            false, // compile PDF
            false, // execute chunks
            false, // auto-indent
            false, // check spelling
            false, // scope tree
            false  // preview from R
            );
   }

   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> juliaCommands = super.getSupportedCommands(commands);

      // these work with R code
      juliaCommands.remove(commands.extractFunction());
      juliaCommands.remove(commands.extractLocalVariable());
      juliaCommands.remove(commands.reformatCode());
      juliaCommands.remove(commands.reformatDocument());
      juliaCommands.remove(commands.renameInScope());
      juliaCommands.remove(commands.profileCode());
      juliaCommands.remove(commands.profileCodeWithoutFocus());
      juliaCommands.remove(commands.showDiagnosticsActiveDocument());
      juliaCommands.remove(commands.showDiagnosticsProject());
      juliaCommands.remove(commands.reindent());

      // these find their code with a scope tree, which Julia's mode lacks
      juliaCommands.remove(commands.executeCurrentFunction());
      juliaCommands.remove(commands.executeCurrentSection());

      // with no code to run, these come off along with the commands that
      // run it, but they only edit comments
      juliaCommands.add(commands.commentUncomment());
      juliaCommands.add(commands.reflowComment());

      return juliaCommands;
   }

   // Julia code runs in the Console's Julia, which the administrator can
   // turn off; Julia files can then only be edited
   @Override
   public boolean canSource()
   {
      return super.canSource() && isJuliaEnabled();
   }

   @Override
   public boolean canExecuteCode()
   {
      return super.canExecuteCode() && isJuliaEnabled();
   }

   @Override
   public boolean canExecuteAllCode()
   {
      return super.canExecuteAllCode() && isJuliaEnabled();
   }

   @Override
   public boolean canExecuteToCurrentLine()
   {
      return super.canExecuteToCurrentLine() && isJuliaEnabled();
   }

   private static boolean isJuliaEnabled()
   {
      return ConsoleLanguageTracker.isJuliaEnabled();
   }

   // A Julia file is included without echo, and can't run as an R job, so
   // the toolbar's source options aren't offered. (Source with Echo stays
   // supported: the Source button runs it when sourceWithEcho is set.)
   @Override
   public boolean canSourceWithEcho()
   {
      return false;
   }
}
