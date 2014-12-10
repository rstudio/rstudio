/*
 * DocumentMode.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
   private static boolean isPositionInMode(DocDisplay docDisplay,
                                           Position position,
                                           String modeString)
   {
      String m = docDisplay.getLanguageMode(position);
      return m != null && m.equals(modeString);
   }
   
   // Markdown Mode
   private static boolean isPositionInMarkdownMode(DocDisplay docDisplay,
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
   private static boolean isPositionInRMode(DocDisplay docDisplay,
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
   private static boolean isPositionInCppMode(DocDisplay docDisplay,
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
   private static boolean isPositionInTexMode(DocDisplay docDisplay,
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

}
