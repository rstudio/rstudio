/*
 * PreviewableFromRFileType.java
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

import java.util.HashSet;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.resources.client.ImageResource;

public class PreviewableFromRFileType extends TextFileType
{
   public PreviewableFromRFileType(String id,
                                   String label,
                                   EditorLanguage editorLanguage,
                                   String defaultExtension,
                                   ImageResource icon,
                                   String previewFunction,
                                   boolean canShowScopeTree)
   {
      super(id, label, editorLanguage, defaultExtension, icon,
            true, true, false, false, false, false, 
            false, false, false, false, false, canShowScopeTree, true);
      
      previewFunction_ = previewFunction;
   }
   
   
   @Override
   public boolean canSource()
   {
      return true;
   }
   
   @Override
   public String createPreviewCommand(String file)
   {
      String cmd = previewFunction_ + "(\"" + file + "\")";
      return cmd;
   }
   
   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      result.add(commands.sourceActiveDocument());
      result.add(commands.sourceActiveDocumentWithEcho());
      return result;
   }
   
   private final String previewFunction_;
}
