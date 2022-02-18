/*
 * PackageLibraryUtils.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.packages.PackagesConstants;

public class PackageLibraryUtils
{
   public enum PackageLibraryType
   {
      None,
      Project,
      User,
      System
   }

   public static PackageLibraryType typeOfLibrary(Session session, 
                                                  String library)
   {
      FileSystemItem projectDir = null;
      SessionInfo sessionInfo = session.getSessionInfo();
      if (sessionInfo != null)
         projectDir = sessionInfo.getActiveProjectDir();
      
      String rLibsUser = sessionInfo.getRLibsUser();
      boolean hasRLibsUser = !StringUtil.isNullOrEmpty(rLibsUser);
      
      // if there's an active project and this package is in its library or
      // the package has no recorded library (i.e. it's not installed), it
      // belongs in the project library
      if (StringUtil.isNullOrEmpty(library) ||
          (projectDir != null && library.startsWith(projectDir.getPath())))
      {
         return PackageLibraryType.Project;
      }
      else if (library.startsWith(FileSystemItem.HOME_PATH) ||
               (hasRLibsUser && library.startsWith(rLibsUser)))
      {
         return PackageLibraryType.User;
      } 
      else
      {
         return PackageLibraryType.System;
      }
   }
         
   public static String nameOfLibraryType(PackageLibraryType type)
   {
      if (type == PackageLibraryType.Project)
         return constants_.projectLibraryText();
      else if (type == PackageLibraryType.User)
         return constants_.userLibraryText();
      else if (type == PackageLibraryType.System)
         return constants_.systemLibraryText();
      return constants_.libraryText();
   }
   
   public static String getLibraryDescription (Session session, String library)
   {
      return nameOfLibraryType(typeOfLibrary(session, library));
   }
   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);
}
