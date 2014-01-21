/*
 * GitHubViewRequest.java
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

package org.rstudio.studio.client.workbench.views.vcs.common.model;

import org.rstudio.core.client.files.FileSystemItem;

public class GitHubViewRequest
{
   public enum ViewType 
   {
      View,
      Blame
   }
   
   public GitHubViewRequest(FileSystemItem file, ViewType type)
   {
      this(file, type, -1, -1);
   }
   
   public GitHubViewRequest(FileSystemItem file,
                            ViewType type,
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

   public ViewType getViewType()
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

   private final FileSystemItem file_;
   private final ViewType type_;
   private final int startLine_;
   private final int endLine_;
}
