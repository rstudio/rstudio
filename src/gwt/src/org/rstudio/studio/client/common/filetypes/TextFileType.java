/*
 * TextFileType.java
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

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.UnicodeLetters;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
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
                boolean canCheckSpelling,
                boolean canShowScopeTree,
                boolean canPreviewFromR)
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
      canShowScopeTree_ = canShowScopeTree;
      canPreviewFromR_ = canPreviewFromR;
   }

   @Override
   public void openFile(FileSystemItem file,
                        FilePosition position,
                        int navMethod,
                        EventBus eventBus)
   {
      eventBus.fireEvent(new OpenSourceFileEvent(file,
                                                 position, 
                                                 this, 
                                                 navMethod));
   }
   
   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      openFile(file, null, NavigationMethods.DEFAULT, eventBus);
   }

   public EditorLanguage getEditorLanguage()
   {
      return editorLanguage_;
   }

   public boolean getWordWrap()
   {
      return wordWrap_;
   }

   public boolean canSource()
   {
      return canExecuteCode_ && !canExecuteChunks_;
   }
   
   public boolean canSourceWithEcho()
   {
      return canSource();
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

   public boolean canCompileNotebook()
   {
      return false;
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

   public boolean canShowScopeTree()
   {
      return canShowScopeTree_;
   }
   
   public boolean canGoNextPrevSection()
   {
      return isRmd() || isRpres();
   }
  
   public boolean canPreviewFromR()
   {
      return canPreviewFromR_;
   }
   
   public boolean isText()
   {
      return FileTypeRegistry.TEXT.getTypeId().equals(getTypeId());
   }

   public boolean isR()
   {
      return FileTypeRegistry.R.getTypeId().equals(getTypeId());
   }
   
   public boolean isRnw()
   {
      return FileTypeRegistry.SWEAVE.getTypeId().equals(getTypeId());
   }
   
   public boolean isRd()
   {
      return FileTypeRegistry.RD.getTypeId().equals(getTypeId());
   }
   
   public boolean isJS()
   {
      return FileTypeRegistry.JS.getTypeId().equals(getTypeId());
   }
   
   public boolean isRmd()
   {
      return FileTypeRegistry.RMARKDOWN.getTypeId().equals(getTypeId());
   }
   
   public boolean isRhtml()
   {
      return FileTypeRegistry.RHTML.getTypeId().equals(getTypeId());
   }
   
   public boolean isRpres()
   {
      return FileTypeRegistry.RPRESENTATION.getTypeId().equals(getTypeId());
   }

   public boolean isSql()
   {
      return FileTypeRegistry.SQL.getTypeId().equals(getTypeId());
   }
   
   public boolean isYaml()
   {
      return FileTypeRegistry.YAML.getTypeId().equals(getTypeId());
   }
   
   public boolean requiresKnit()
   {
      return FileTypeRegistry.RMARKDOWN.getTypeId().equals(getTypeId()) ||
             FileTypeRegistry.RHTML.getTypeId().equals(getTypeId()) ||
             FileTypeRegistry.RPRESENTATION.getTypeId().equals(getTypeId());
   }
   
   public boolean isMarkdown()
   {
      return FileTypeRegistry.RMARKDOWN.getTypeId().equals(getTypeId()) ||
             FileTypeRegistry.MARKDOWN.getTypeId().equals(getTypeId());
   }
   
   public boolean isPlainMarkdown()
   {
      return FileTypeRegistry.MARKDOWN.getTypeId().equals(getTypeId());
   }
   
   public boolean isRNotebook()
   {
      return FileTypeRegistry.RNOTEBOOK.getTypeId().equals(getTypeId());
   }
   
   public boolean isC()
   {
      return EditorLanguage.LANG_CPP.equals(getEditorLanguage());
   }
   
   public boolean isPython()
   {
      return EditorLanguage.LANG_PYTHON.equals(getEditorLanguage());
   }
   
   public boolean isCpp()
   {
      return false;
   }
   
   public boolean isStan()
   {
      return EditorLanguage.LANG_STAN.equals(getEditorLanguage());
   }
   
   public String getPreviewButtonText()
   {
      return "Preview";
   }
   
   public String createPreviewCommand(String file)
   {
      return null;
   }
   
   public boolean isScript()
   {
      return false;
   }
   
   public String getScriptInterpreter()
   {
      return null;
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
      results.add(commands.vcsViewOnGitHub());
      results.add(commands.vcsBlameOnGitHub());
      results.add(commands.goToLine());
      results.add(commands.expandSelection());
      results.add(commands.shrinkSelection());
      
      if (isJS())
      {
         results.add(commands.previewJS());
      }

      if (isSql())
      {
         results.add(commands.previewSql());
      }
      
      if (isYaml())
      {
         results.add(commands.commentUncomment());
      }
      
      if ((canExecuteCode() && !isScript()) || isC())
      {
         results.add(commands.reindent());
         results.add(commands.showDiagnosticsActiveDocument());
      }
      
      if (canExecuteCode() && !isC() && !isScript())
      {
         results.add(commands.executeCurrentFunction());
      }
      
      if (canExecuteCode())
      {
         results.add(commands.executeCode());
         results.add(commands.executeCodeWithoutFocus());
         
         if (!isScript())
         {
            results.add(commands.executeLastCode());
            results.add(commands.extractFunction());
            results.add(commands.extractLocalVariable());
            results.add(commands.commentUncomment());
            results.add(commands.reflowComment());
            results.add(commands.reformatCode());
            results.add(commands.renameInScope());
            results.add(commands.profileCode());
            results.add(commands.profileCodeWithoutFocus());
         }
      }
      
      if (canExecuteAllCode())
      {
         results.add(commands.executeAllCode());
         results.add(commands.sourceActiveDocument());
         // file types with chunks take the Cmd+Shift+Enter shortcut
         // for run current chunk
         if (!canExecuteChunks())
            results.add(commands.sourceActiveDocumentWithEcho());
      }
      
      if (canExecuteToCurrentLine())
      {
         results.add(commands.executeToCurrentLine());
         results.add(commands.executeFromCurrentLine());
         results.add(commands.executeCurrentSection());
         results.add(commands.profileCode());
      }
      if (canKnitToHTML())
      {
         results.add(commands.editRmdFormatOptions());
         results.add(commands.knitWithParameters());
         results.add(commands.clearKnitrCache());
         results.add(commands.restartRClearOutput());
         results.add(commands.restartRRunAllChunks());
         results.add(commands.notebookCollapseAllOutput());
         results.add(commands.notebookExpandAllOutput());
         results.add(commands.executeSetupChunk());
      }
      if (canKnitToHTML() || canCompileNotebook())
      {
         results.add(commands.knitDocument());
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
      if (canCompileNotebook())
      {
         results.add(commands.compileNotebook());
      }
      if (canExecuteChunks())
      {
         results.add(commands.insertChunk());
         results.add(commands.executePreviousChunks());
         results.add(commands.executeSubsequentChunks());
         results.add(commands.executeCurrentChunk());
         results.add(commands.executeNextChunk());
         results.add(commands.runSelectionAsJob());
         results.add(commands.runSelectionAsLauncherJob());
      }
      if (isMarkdown())
      {
         results.add(commands.toggleRmdVisualMode());
         results.add(commands.enableProsemirrorDevTools());
      }
      if (canCheckSpelling())
      {
         results.add(commands.checkSpelling());
      }
      if (canShowScopeTree())
      {
         results.add(commands.toggleDocumentOutline());
      }

      results.add(commands.wordCount());
      results.add(commands.goToNextSection());
      results.add(commands.goToPrevSection());
      results.add(commands.goToNextChunk());
      results.add(commands.goToPrevChunk());
      results.add(commands.findReplace());
      results.add(commands.findNext());
      results.add(commands.findPrevious());
      results.add(commands.findFromSelection());
      results.add(commands.replaceAndFind());
      results.add(commands.setWorkingDirToActiveDoc());
      results.add(commands.debugDumpContents());
      results.add(commands.debugImportDump());
      results.add(commands.popoutDoc());
      if (!SourceWindowManager.isMainSourceWindow())
         results.add(commands.returnDocToMain());

      if (isR())
      {
         results.add(commands.sourceAsLauncherJob());
         results.add(commands.sourceAsJob());
         results.add(commands.runSelectionAsJob());
         results.add(commands.runSelectionAsLauncherJob());
      }

      results.add(commands.sendToTerminal());
      results.add(commands.sendFilenameToTerminal());
      results.add(commands.openNewTerminalAtEditorLocation());
      results.add(commands.toggleSoftWrapMode());

      return results;
   }

   public String getDefaultExtension()
   {
      return defaultExtension_;
   }

   public TokenPredicate getTokenPredicate()
   {
      return (token, row, column) ->
      {
         if (reNospellType_.match(token.getType(), 0) != null) {
            return false;
         }

         return reTextType_.match(token.getType(), 0) != null ||
            reStringType_.match(token.getType(), 0) != null ||
            reHeaderType_.match(token.getType(), 0) != null ||
            reCommentType_.match(token.getType(), 0) != null;
      };
   }

   // default to only returning comments and text, override in subclasses
   // for more or less specificity
   public TokenPredicate getSpellCheckTokenPredicate()
   {
      return (token, row, column) ->
      {
         if (reNospellType_.match(token.getType(), 0) != null) {
            return false;
         }

         return (reCommentType_.match(token.getType(), 0) != null ||
                 reTextType_.match(token.getType(), 0) != null) &&
                 reKeywordType_.match(token.getType(), 0) == null &&
                 reIdentifierType_.match(token.getType(), 0) == null;
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
            else if (c == '\'' || c == '’')
               return CharClass.Boundary;
            else
               return CharClass.NonWord;
         }
      };
   }

   /**
    * Returns a regex pattern that will match against the beginning of
    * Rnw lines, right up to the index where options begin.
    */
   public Pattern getRnwStartPatternBegin()
   {
      return null;
   }

   public Pattern getRnwStartPatternEnd()
   {
      return null;
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
   private final boolean canShowScopeTree_;
   private final boolean canPreviewFromR_;
   private final String defaultExtension_;

   protected static Pattern reTextType_ = Pattern.create("\\btext\\b");
   protected static Pattern reStringType_ = Pattern.create("\\bstring\\b");
   protected static Pattern reHeaderType_ = Pattern.create("\\bheading\\b");
   protected static Pattern reNospellType_ = Pattern.create("\\bnospell\\b");
   protected static Pattern reCommentType_ = Pattern.create("\\bcomment\\b");
   protected static Pattern reKeywordType_ = Pattern.create("\\bkeyword\\b");
   protected static Pattern reIdentifierType_ = Pattern.create("\\bidentifier\\b");
}
