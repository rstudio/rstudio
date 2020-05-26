/*
 * ScriptFileType.java
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

import com.google.gwt.resources.client.ImageResource;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

public class ScriptFileType extends TextFileType
{
   public ScriptFileType(String id, 
                         String label,
                         EditorLanguage language,
                         String ext, 
                         ImageResource icon,
                         String interpreter,
                         boolean canExecuteCode,
                         boolean windowsCompatible)
   {
      super(id, label, language, ext, icon,
            false, false, canExecuteCode, false, false, false, 
            false, false, false, false, false, false, false);
      interpreter_ = interpreter;
      windowsCompatible_ = windowsCompatible;
   }
   
   
   @Override
   public boolean canSource()
   {
      if (BrowseCap.isWindowsDesktop())
         return windowsCompatible_;
      else
         return true;
   }
 
   @Override
   public boolean canSourceWithEcho()
   {
      return canSource();
   }
   
   @Override
   public boolean isScript()
   {
      return true;
   }
   
   @Override
   public String getScriptInterpreter()
   {
      return interpreter_;
   }
   
   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      result.add(commands.commentUncomment());
      if (canSource())
         result.add(commands.sourceActiveDocument());
      if (canSourceWithEcho())
         result.add(commands.sourceActiveDocumentWithEcho());
      return result;
   }
   
   private final String interpreter_;
   private final boolean windowsCompatible_;
}
