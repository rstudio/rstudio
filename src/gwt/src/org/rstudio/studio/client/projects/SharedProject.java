/*
 * SharedProject.java
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
package org.rstudio.studio.client.projects;

import org.rstudio.studio.client.projects.model.ProjectUser;

import com.google.inject.Singleton;

@Singleton
public class SharedProject
{
   public SharedProject()
   {
   }
   
   public ProjectUser getFollowingUser()
   {
      return null;
   }
   
   public ProjectUser popCursorSync(String path)
   {
      return null;
   }
   
   public void reportCollabDisconnected(String path, String id)
   {
   }
}
