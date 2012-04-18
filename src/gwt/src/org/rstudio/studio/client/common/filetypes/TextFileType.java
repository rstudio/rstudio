/*
 * TextFileType.java
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

import com.google.gwt.resources.client.ImageResource;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.UnicodeLetters;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;

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
                boolean canPreviewHTML,
                boolean canKnitToHTML,
                boolean canCompilePDF,
                boolean canExecuteChunks,
                boolean canAutoIndent,
                boolean canCheckSpelling)
   {
      super(id, label, defaultIcon);
      editorLanguage_ = editorLanguage;
      defaultExtension_ = defaultExtension;
      wordWrap_ = wordWrap;
      canSourceOnSave_ = canSourceOnSave;
      canExecuteCode_ = canExecuteCode;
      canExecuteAllCode_ = canExecuteAllCode;
      canExecuteToCurrentLine_ = canExecuteToCurrentLine;
      canKnitToHTML_ = canKnitToHTML;
      canPreviewHTML_ = canPreviewHTML;
      canCompilePDF_ = canCompilePDF;
      canExecuteChunks_ = canExecuteChunks;
      canAutoIndent_ = canAutoIndent;
      canCheckSpelling_ = canCheckSpelling;
   }

   @Override
   public void openFile(FileSystemItem file,
                        FilePosition position,
                        EventBus eventBus)
   {
      eventBus.fireEvent(new OpenSourceFileEvent(file, position, this));
   }
   
   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      openFile(file, null, eventBus);
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
   
   public boolean canKnitToHTML()
   {
      return canKnitToHTML_;
   }
   
   public boolean canPreviewHTML()
   {
      return canPreviewHTML_;
   }

   public boolean canCompilePDF()
   {
      return canCompilePDF_;
   }
   
   public boolean canAuthorContent()
   {
      return canKnitToHTML() || canPreviewHTML() || canCompilePDF();
   }
   
   public boolean canExecuteChunks()
   {
      return canExecuteChunks_;
   }
   
   public boolean canAutoIndent()
   {
      return canAutoIndent_;
   }
   
   public boolean canCheckSpelling()
   {
      return canCheckSpelling_;
   }
   
   public boolean isRnw()
   {
      return FileTypeRegistry.SWEAVE.getTypeId().equals(getTypeId());
   }
   
   public boolean requiresKnit()
   {
      return FileTypeRegistry.RMARKDOWN.getTypeId().equals(getTypeId()) ||
             FileTypeRegistry.RHTML.getTypeId().equals(getTypeId());
   }
   
   public boolean isMarkdown()
   {
      return FileTypeRegistry.RMARKDOWN.getTypeId().equals(getTypeId()) ||
             FileTypeRegistry.MARKDOWN.getTypeId().equals(getTypeId());
   }

   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> results = new HashSet<AppCommand>();
      results.add(commands.saveSourceDoc());
      results.add(commands.reopenSourceDocWithEncoding());
      results.add(commands.saveSourceDocAs());
      results.add(commands.saveSourceDocWithEncoding());
      results.add(commands.printSourceDoc());
      results.add(commands.vcsFileLog());
      results.add(commands.vcsFileDiff());
      results.add(commands.vcsFileRevert());
      results.add(commands.goToLine());
      if (canExecuteCode())
      {
         results.add(commands.executeCode());
         results.add(commands.executeLastCode());
         results.add(commands.extractFunction());
         results.add(commands.commentUncomment());
         results.add(commands.reindent());
         results.add(commands.reflowComment());
      }
      if (canExecuteAllCode())
      {
         results.add(commands.executeAllCode());
         results.add(commands.sourceActiveDocument());
         results.add(commands.sourceActiveDocumentWithEcho());
      }
      if (canExecuteToCurrentLine())
      {
         results.add(commands.executeToCurrentLine());
         results.add(commands.executeFromCurrentLine());
         results.add(commands.executeCurrentFunction());
      }
      if (canKnitToHTML())
      {
         results.add(commands.knitToHTML());
      }
      if (canPreviewHTML())
      {
         results.add(commands.previewHTML());
      }
      if (canCompilePDF())
      {
         results.add(commands.compilePDF());
         results.add(commands.synctexSearch());
      }
      if (canExecuteChunks())
      {
         results.add(commands.insertChunk());
         results.add(commands.executeCurrentChunk());
         results.add(commands.executeNextChunk());
      }
      if (canCheckSpelling())
      {
         results.add(commands.checkSpelling());
      }
      results.add(commands.findReplace());
      results.add(commands.setWorkingDirToActiveDoc());
      results.add(commands.debugForceTopsToZero());
      results.add(commands.debugDumpContents());
      results.add(commands.debugImportDump());
      return results;
   }

   public String getDefaultExtension()
   {
      return defaultExtension_;
   }

   public TokenPredicate getTokenPredicate()
   {
      return new TokenPredicate()
      {
         @Override
         public boolean test(Token token, int row, int column)
         {
            return reTextType_.match(token.getType(), 0) != null;
         }
      };
   }

   public CharClassifier getCharPredicate()
   {
      return new CharClassifier()
      {
         @Override
         public CharClass classify(char c)
         {
            if (UnicodeLetters.isLetter(c))
               return CharClass.Word;
            else if (c == '\'')
               return CharClass.Boundary;
            else
               return CharClass.NonWord;
         }
      };
   }

   private final EditorLanguage editorLanguage_;
   private final boolean wordWrap_;
   private final boolean canSourceOnSave_;
   private final boolean canExecuteCode_;
   private final boolean canExecuteAllCode_;
   private final boolean canExecuteToCurrentLine_;
   private final boolean canPreviewHTML_;
   private final boolean canKnitToHTML_;
   private final boolean canCompilePDF_;
   private final boolean canExecuteChunks_;
   private final boolean canAutoIndent_;
   private final boolean canCheckSpelling_;
   private final String defaultExtension_;

   private static Pattern reTextType_ = Pattern.create("\\btext\\b");
}
