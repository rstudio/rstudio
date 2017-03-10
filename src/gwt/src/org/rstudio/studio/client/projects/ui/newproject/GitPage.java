/*
 * GitPage.java
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

public class GitPage extends VersionControlPage
{
   public GitPage()
   {
      super("Git", 
            "Clone a project from a Git repository",
            "Clone Git Repository",
            new ImageResource2x(NewProjectResources.INSTANCE.gitIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.gitIconLarge2x()));
   }
   
   @Override
   protected String getVcsId()
   {
      return VCSConstants.GIT_ID;
   }

   @Override
   protected String guessRepoDir(String url)
   {
      /*
       * Strip trailing spaces, slashes and /.git
       */
      while (url.endsWith("/") || url.endsWith(" ") || url.endsWith("\t"))
         url = url.substring(0, url.length() - 1);
      if (url.endsWith("/.git"))
      {
         url = url.substring(0, url.length() - 5);
         while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);
      }

      /*
       * Find last component, but be prepared that repo could have
       * the form  "remote.example.com:foo.git", i.e. no slash
       * in the directory part.
       */
      url = url.replaceFirst(".*[:/]", ""); // greedy

      /*
       * Strip .{bundle,git}.
       */
      url = url.replaceAll(".(bundle|git)$", "");
      url = url.replaceAll("[\u0000-\u0020]+", " ");
      url = url.trim();
      return url;
   }
}
