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

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

public class DocumentMode
{
   public static boolean isCursorInMarkdownMode(DocDisplay docDisplay)
   {
      if (docDisplay.getFileType().isPlainMarkdown())
         return true;
      
      String m = docDisplay.getLanguageMode(
            docDisplay.getCursorPosition());
      return m != null && m.equals(FileType.MARKDOWN_LANG_MODE);
   }
   
   public static boolean isCursorInRMode(DocDisplay docDisplay)
   {
      if (docDisplay.getFileType().isR())
            return true;
      
      String m = docDisplay.getLanguageMode(
            docDisplay.getCursorPosition());
      return m != null && m.equals(FileType.R_LANG_MODE);
   }
   
   
   public static boolean isCursorInCppMode(DocDisplay docDisplay)
   {
      String m = docDisplay.getLanguageMode(
            docDisplay.getCursorPosition());
      
      // the default mode is Cpp in C++ documents -- check that first. Note that
      // because we embed other modes in C++ documents (R) we need to check
      // this first
      if (docDisplay.getFileType().isCpp() || docDisplay.getFileType().isC())
         return StringUtil.isNullOrEmpty(m);
      
      // otherwise, C++ must be an embedded mode -- check to see if we got
      // an actual mode at the cursor position
      return m != null && m.equals(FileType.C_CPP_LANG_MODE);
   }
   
   public static boolean isCursorInTexMode(DocDisplay docDisplay)
   {
      TextFileType fileType = docDisplay.getFileType();
      if (fileType.canCompilePDF())
      {
         if (fileType.isRnw())
         {
            return FileType.TEX_LANG_MODE.equals(
               docDisplay.getLanguageMode(docDisplay.getCursorPosition()));
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
   
   

}
