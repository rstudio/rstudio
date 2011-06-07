/*
 * TextFileType.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.HashSet;

public class TextFileType extends EditableFileType
{
   TextFileType(String id,
                String label,
                EditorLanguage editorLanguage,
                String defaultExtension,
                ImageResource defaultIcon,
                boolean wordWrap,
                boolean canSourceOnSave,
                boolean canExecuteCode,
                boolean canExecuteAllCode,
                boolean canExecuteToCurrentLine,
                boolean canCompilePDF)
   {
      super(id, label, defaultIcon);
      editorLanguage_ = editorLanguage;
      defaultExtension_ = defaultExtension;
      wordWrap_ = wordWrap;
      canSourceOnSave_ = canSourceOnSave;
      canExecuteCode_ = canExecuteCode;
      canExecuteAllCode_ = canExecuteAllCode;
      canExecuteToCurrentLine_ = canExecuteToCurrentLine;
      canCompilePDF_ = canCompilePDF;
   }

   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      eventBus.fireEvent(new OpenSourceFileEvent(file, this));
   }

   public EditorLanguage getEditorLanguage()
   {
      return editorLanguage_;
   }

   public boolean getWordWrap()
   {
      return wordWrap_;
   }

   public boolean canSourceOnSave()
   {
      return canSourceOnSave_;
   }

   public boolean canExecuteCode()
   {
      return canExecuteCode_;
   }

   public boolean canExecuteAllCode()
   {
      return canExecuteAllCode_;
   }

   public boolean canExecuteToCurrentLine()
   {
      return canExecuteToCurrentLine_;
   }

   public boolean canCompilePDF()
   {
      return canCompilePDF_;
   }

   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> results = new HashSet<AppCommand>();
      results.add(commands.saveSourceDoc());
      results.add(commands.reopenSourceDocWithEncoding());
      results.add(commands.saveSourceDocAs());
      results.add(commands.saveSourceDocWithEncoding());
      results.add(commands.printSourceDoc());
      if (canExecuteCode())
      {
         results.add(commands.executeCode());
         results.add(commands.extractFunction());
         results.add(commands.commentUncomment());
      }
      if (canExecuteAllCode())
      {
         results.add(commands.executeAllCode());
         results.add(commands.sourceActiveDocument());
      }
      if (canExecuteToCurrentLine())
      {
         results.add(commands.executeToCurrentLine());
      }
      if (canCompilePDF())
      {
         results.add(commands.compilePDF());
         results.add(commands.publishPDF());
      }
      results.add(commands.findReplace());
      results.add(commands.setWorkingDirToActiveDoc());
      return results;
   }

   public String getDefaultExtension()
   {
      return defaultExtension_;
   }

   private final EditorLanguage editorLanguage_;
   private final boolean wordWrap_;
   private final boolean canSourceOnSave_;
   private final boolean canExecuteCode_;
   private final boolean canExecuteAllCode_;
   private final boolean canExecuteToCurrentLine_;
   private final boolean canCompilePDF_;
   private final String defaultExtension_;
}
