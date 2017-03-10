/*
 * SvnPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.vcs.VCSConstants;

public class SvnPage extends VersionControlPage
{
   public SvnPage()
   {
      super("Subversion", 
            "Checkout a project from a Subversion repository",
            "Checkout Subversion Repository",
            new ImageResource2x(NewProjectResources.INSTANCE.svnIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.svnIconLarge2x()));
   }
   

   @Override
   protected String getVcsId()
   {
      return VCSConstants.SVN_ID;
   }

   @Override
   protected boolean includeCredentials()
   {
      return true;
   }
   
   
   @Override 
   protected String guessRepoDir(String url)
   {
      // Strip trailing spaces and slashes
      while (url.endsWith("/") || url.endsWith(" ") || url.endsWith("\t"))
         url = url.substring(0, url.length() - 1);
      
      // Find last component
      url = url.replaceFirst(".*[/]", ""); // greedy

      url = url.replaceAll("[\u0000-\u0020]+", " ");
      url = url.trim();
      
      // Suppress if it is "trunk"
      if (url.equals("trunk"))
         url = "";
      
      return url;
     
   }
}
