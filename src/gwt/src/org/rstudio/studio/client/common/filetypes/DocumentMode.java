/*
 * DocumentMode.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class DocumentMode
{
   public enum Mode
   {
      R,
      PYTHON,
      C_CPP,
      MARKDOWN,
      SQL,
      STAN,
      TEX,
      UNKNOWN
   }
   
   public static Mode getModeForPosition(DocDisplay docDisplay,
                                         Position position)
   {
      if (isPositionInRMode(docDisplay, position))
         return Mode.R;
      else if (isPositionInPythonMode(docDisplay, position))
         return Mode.PYTHON;
      else if (isPositionInCppMode(docDisplay, position))
         return Mode.C_CPP;
      else if (isPositionInMarkdownMode(docDisplay, position))
         return Mode.MARKDOWN;
      else if (isPositionInTexMode(docDisplay, position))
         return Mode.TEX;
      else if (isPositionInSqlMode(docDisplay, position))
         return Mode.SQL;
      else if (isPositionInStanMode(docDisplay, position))
         return Mode.STAN;
      else
         return Mode.UNKNOWN;
   }
   
   public static Mode getModeForCursorPosition(DocDisplay docDisplay)
   {
      return getModeForPosition(docDisplay, docDisplay.getCursorPosition());
   }
   
   private static boolean isPositionInMode(DocDisplay docDisplay,
                                           Position position,
                                           String modeString)
   {
      String m = docDisplay.getLanguageMode(position);
      return m != null && m.equals(modeString);
   }
   
   // Markdown Mode
   public static boolean isPositionInMarkdownMode(DocDisplay docDisplay,
                                                   Position position)
   {
      if (docDisplay.getFileType().isPlainMarkdown())
         return true;
      
      return isPositionInMode(
            docDisplay,
            position,
            FileType.MARKDOWN_LANG_MODE);
      
   }
   
   public static boolean isCursorInMarkdownMode(DocDisplay docDisplay)
   {
      return isPositionInMarkdownMode(
            docDisplay,
            docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInMarkdownMode(DocDisplay docDisplay)
   {
      return isPositionInMarkdownMode(
            docDisplay,
            docDisplay.getSelectionStart()) &&
            isPositionInMarkdownMode(
                  docDisplay,
                  docDisplay.getSelectionEnd());
   }
   
   // R Mode
   public static boolean isPositionInRMode(DocDisplay docDisplay,
                                            Position position)
   {
      if (docDisplay.getFileType().isR())
         return true;
      
      return isPositionInMode(
            docDisplay,
            position,
            FileType.R_LANG_MODE);
      
   }
   public static boolean isCursorInRMode(DocDisplay docDisplay)
   {
      return isPositionInRMode(
            docDisplay,
            docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInRMode(DocDisplay docDisplay)
   {
      return isPositionInRMode(docDisplay, docDisplay.getSelectionStart()) &&
             isPositionInRMode(docDisplay, docDisplay.getSelectionEnd());
   }
   
   // C++ Mode
   public static boolean isPositionInCppMode(DocDisplay docDisplay,
                                              Position position)
   {    
      return isPositionInMode(
            docDisplay,
            position,
            FileType.C_CPP_LANG_MODE);
   }
   
   public static boolean isCursorInCppMode(DocDisplay docDisplay)
   {
      return isPositionInCppMode(docDisplay, docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInCppMode(DocDisplay docDisplay)
   {
      return isPositionInCppMode(docDisplay, docDisplay.getSelectionStart()) &&
             isPositionInCppMode(docDisplay, docDisplay.getSelectionEnd());
   }
   
   // TeX Mode
   public static boolean isPositionInTexMode(DocDisplay docDisplay,
                                               Position position)
   {
      TextFileType fileType = docDisplay.getFileType();
      if (fileType.canCompilePDF())
      {
         if (fileType.isRnw())
         {
            return FileType.TEX_LANG_MODE.equals(
               docDisplay.getLanguageMode(position));
         }
         else
         {
            return true;
         }
      }
      else
      {
         return false;
      }
      
   }
   
   public static boolean isCursorInTexMode(DocDisplay docDisplay)
   {
      return isPositionInTexMode(docDisplay, docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInTexMode(DocDisplay docDisplay)
   {
      return isPositionInTexMode(docDisplay, docDisplay.getSelectionStart()) &&
             isPositionInTexMode(docDisplay, docDisplay.getSelectionEnd());
   }
   
   // Python Mode
   public static boolean isPositionInPythonMode(DocDisplay docDisplay,
                                                 Position position)
   {
      if (docDisplay.getFileType().isPython())
         return true;
      
      return isPositionInMode(
            docDisplay,
            position,
            FileType.PYTHON_LANG_MODE);
      
   }
   
   public static boolean isCursorInPythonMode(DocDisplay docDisplay)
   {
      return isPositionInPythonMode(
            docDisplay,
            docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInPythonMode(DocDisplay docDisplay)
   {
      return isPositionInPythonMode(docDisplay, docDisplay.getSelectionStart()) &&
             isPositionInPythonMode(docDisplay, docDisplay.getSelectionEnd());
   }
   
   public static boolean isPositionInSqlMode(DocDisplay docDisplay,
                                             Position position)
   {
      if (docDisplay.getFileType().isSql())
         return true;
      
      return isPositionInMode(docDisplay, position, FileType.SQL_LANG_MODE);
   }
   
   // Stan Mode
   public static boolean isPositionInStanMode(DocDisplay docDisplay,
                                              Position position)
   {
      if (docDisplay.getFileType().isStan())
         return true;
      
      return isPositionInMode(
            docDisplay,
            position,
            FileType.STAN_LANG_MODE);
      
   }
   
   public static boolean isCursorInStanMode(DocDisplay docDisplay)
   {
      return isPositionInStanMode(
            docDisplay,
            docDisplay.getCursorPosition());
   }
   
   public static boolean isSelectionInStanMode(DocDisplay docDisplay)
   {
      return isPositionInStanMode(docDisplay, docDisplay.getSelectionStart()) &&
             isPositionInStanMode(docDisplay, docDisplay.getSelectionEnd());
   }

}
