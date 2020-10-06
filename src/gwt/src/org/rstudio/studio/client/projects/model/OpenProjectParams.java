/*
 * OpenProjectParams.java
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
package org.rstudio.studio.client.projects.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.model.RVersionSpec;

public class OpenProjectParams
{
   public OpenProjectParams(FileSystemItem projectFile, 
                            RVersionSpec rVersion,
                            boolean inNewSession)
   {
      projectFile_ = projectFile;
      rVersion_ = rVersion;
      inNewSession_ = inNewSession;
   }
   
   public FileSystemItem getProjectFile()
   {
      return projectFile_;
   }
   
   public RVersionSpec getRVersion()
   {
      return rVersion_;
   }
   
   public boolean inNewSession()
   {
      return inNewSession_;
   }

   private final FileSystemItem projectFile_;
   private final RVersionSpec rVersion_;
   private final boolean inNewSession_;
}
