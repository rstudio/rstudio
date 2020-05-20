/*
 * GitHubViewRequest.java
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

package org.rstudio.studio.client.workbench.views.vcs.common.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;

@JavaScriptSerializable
public class GitHubViewRequest
{
   public GitHubViewRequest()
   {
   }

   public GitHubViewRequest(FileSystemItem file, int type)
   {
      this(file, type, -1, -1);
   }
   
   public GitHubViewRequest(FileSystemItem file,
                            int type,
                            int startLine, 
                            int endLine)
   {
      file_ = file;
      type_ = type;
      startLine_ = startLine;
      endLine_ = endLine;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   public int getViewType()
   {
      return type_;
   }

   public int getStartLine()
   {
      return startLine_;
   }

   public int getEndLine()
   {
      return endLine_;
   }

   private FileSystemItem file_;
   private int type_;
   private int startLine_;
   private int endLine_;
   
   public final static int VCS_VIEW = 0;
   public final static int VCS_BLAME = 1;
}
