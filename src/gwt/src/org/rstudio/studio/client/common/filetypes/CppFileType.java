/*
 * CppFileType.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import java.util.HashSet;

import com.google.gwt.resources.client.ImageResource;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

public class CppFileType extends TextFileType
{
   CppFileType(String id, String ext, ImageResource icon, boolean isCpp)
   {
      super(id, "C/C++", EditorLanguage.LANG_CPP, ext, icon,
            false, false, false, false, false, false, 
            false, false, false, false, false, false);
      
      isCpp_ = isCpp;
   }
   
   @Override
   public boolean isCpp()
   {
      return isCpp_;
   }
   
   
   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      if (isCpp())
      {
         result.add(commands.commentUncomment());
         result.add(commands.reflowComment());
      }
      return result;
   }
   
   
   private final boolean isCpp_;
}