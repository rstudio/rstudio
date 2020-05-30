/*
 * ImportFileSettingsDialogResult.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

public class ImportFileSettingsDialogResult
{
   public ImportFileSettingsDialogResult(ImportFileSettings settings,
                 boolean defaultStringsAsFactors)
   {
      settings_ = settings;
      defaultStringsAsFactors_ = defaultStringsAsFactors;
   }
   
   public ImportFileSettings getSettings()
   {
      return settings_;
   }
   
   public boolean getDefaultStringsAsFactors()
   {
      return defaultStringsAsFactors_;
   }
   
   private final ImportFileSettings settings_;
   private final boolean defaultStringsAsFactors_;
}
