/*
 * PackageInstallRequest.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.model;

public class PackageInstallRequest 
{
   public PackageInstallRequest(String packageName,
                                PackageInstallOptions options)
   {
      packageName_ = packageName;
      options_ = options;
   }
  
   public String getPackageName()
   {
      return packageName_;
   }
   
   public PackageInstallOptions getOptions()
   {
      return options_;
   }
   
   
   private final String packageName_;
   private final PackageInstallOptions options_;

   
}
