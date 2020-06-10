/*
 * NewProjectInput.java
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

public class NewProjectInput
{
   public NewProjectInput(FileSystemItem defaultNewProjectLocation, 
                          NewProjectContext context)
   {
      defaultNewProjectLocation_ = defaultNewProjectLocation;
      context_ = context;
   }
   
   public FileSystemItem getDefaultNewProjectLocation()
   {
      return defaultNewProjectLocation_;
   }
   
   public NewProjectContext getContext()
   {
      return context_;
   }
    
   private final FileSystemItem defaultNewProjectLocation_;
   private final NewProjectContext context_;
}
