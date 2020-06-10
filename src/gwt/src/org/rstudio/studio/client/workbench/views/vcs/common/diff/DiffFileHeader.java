/*
 * DiffFileHeader.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

import java.util.ArrayList;

public class DiffFileHeader
{
   public DiffFileHeader(ArrayList<String> headerLines,
                         String oldFile,
                         String newFile)
   {
      headerLines_ = headerLines;
      oldFile_ = oldFile.replaceFirst("^a/", "").replaceFirst("\\s\\(revision [0-9]+\\)", "");
      newFile_ = newFile.replaceFirst("^b/", "").replaceFirst("\\s\\(revision [0-9]+\\)", "");
   }

   public String getDescription()
   {
      if (oldFile_ == newFile_)
         return oldFile_;

      if (oldFile_ == "/dev/null")
         return newFile_;
      if (newFile_ == "/dev/null")
         return oldFile_;
      return oldFile_ + " => " + newFile_;
   }

   @SuppressWarnings("unused")
   private final ArrayList<String> headerLines_;
   private final String oldFile_;
   private final String newFile_;
}
