/*
 * RWebContentFileType.java
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
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.HashSet;

public class RWebContentFileType extends TextFileType
{
   RWebContentFileType(String id,
                       String label,
                       EditorLanguage editorLanguage,
                       String defaultExtension,
                       ImageResource icon,
                       boolean isMarkdown)
   {
      this(id, label, editorLanguage, defaultExtension, icon, isMarkdown, true);
   }
   
   RWebContentFileType(String id,
                       String label,
                       EditorLanguage editorLanguage,
                       String defaultExtension,
                       ImageResource icon,
                       boolean isMarkdown,
                       boolean previewIsKnit)
   {
      super(id, 
            label, 
            editorLanguage, 
            defaultExtension,
            icon,
            true,    // word-wrap
            false, 
            true, 
            true, 
            false,
            !previewIsKnit,    // preview-html
            previewIsKnit,     // knit-html
            false, 
            true,
            false,
            true,
            true,
            false);
      
      isMarkdown_ = isMarkdown;
   }
   
   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      result.add(commands.jumpTo());
      result.add(commands.jumpToMatching());
      result.add(commands.goToHelp());
      result.add(commands.goToDefinition());
      result.add(commands.insertRoxygenSkeleton());
      return result;
   }

   @Override
   public boolean getWordWrap()
   {
      return RStudioGinjector.INSTANCE.getUserPrefs().softWrapRmdFiles().getValue();
   }

   @Override
   public Pattern getRnwStartPatternBegin()
   {
      if (isMarkdown_)
         return RNW_START_PATTERN_MD;
      else
         return RNW_START_PATTERN_HTML;
   }

   @Override
   public Pattern getRnwStartPatternEnd()
   {
      return null;
   }

   private final boolean isMarkdown_;

   private static final Pattern RNW_START_PATTERN_MD =
                                       Pattern.create("^`{3,}\\s*\\{r");
   private static final Pattern RNW_START_PATTERN_HTML =
                                Pattern.create("^\\<!--\\s*begin.rcode\\s*");

}
