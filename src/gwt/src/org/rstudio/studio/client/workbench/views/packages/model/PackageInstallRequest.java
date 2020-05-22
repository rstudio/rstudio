/*
 * PackageInstallRequest.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import java.util.List;

import org.rstudio.core.client.files.FileSystemItem;

public class PackageInstallRequest 
{
   public PackageInstallRequest(List<String> packages,
                                PackageInstallOptions options)
   {
      this(packages, null, options);
   }
   
   public PackageInstallRequest(FileSystemItem localPackage,
                                 PackageInstallOptions options)
   {
      this(null, localPackage, options);
   }
  
   private PackageInstallRequest(List<String> packages,
                                 FileSystemItem localPackage,
                                 PackageInstallOptions options)
   {
      packages_ = packages;
      localPackage_ = localPackage;
      options_ = options;
   }
   
   public List<String> getPackages()
   {
      return packages_;
   }
   
   public FileSystemItem getLocalPackage()
   {
      return localPackage_;
   }
   
   public PackageInstallOptions getOptions()
   {
      return options_;
   }
   
   
   private final List<String> packages_;
   private final FileSystemItem localPackage_;
   private final PackageInstallOptions options_;

   
}
