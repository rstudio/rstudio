/*
 * BuildToolsRoxygenOptions.java
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

package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.studio.client.projects.model.RProjectAutoRoxygenizeOptions;

public class BuildToolsRoxygenOptions
{
   public BuildToolsRoxygenOptions(boolean rocletRd,
                                   boolean rocletCollate,
                                   boolean rocletNamespace,
                                   RProjectAutoRoxygenizeOptions autoRoxygenize)
   {
      rocletRd_ = rocletRd;
      rocletCollate_ = rocletCollate;
      rocletNamespace_ = rocletNamespace;
      autoRoxygenize_ = autoRoxygenize;
   }
   
   public boolean hasActiveRoclet()
   {
      return getRocletRd() || getRocletCollate() || getRocletNamespace();
   }
   
   public void clearRoclets()
   {
      setRocletRd(false);
      setRocletCollate(false);
      setRocletNamespace(false);
   }
   
   public boolean getRocletRd()
   {
      return rocletRd_;
   }
   
   public void setRocletRd(boolean rocletRd)
   {
      rocletRd_ = rocletRd;
   }
   
   public boolean getRocletCollate()
   {
      return rocletCollate_;
   }
   
   public void setRocletCollate(boolean rocletCollate)
   {
      rocletCollate_ = rocletCollate;
   }
   
   public boolean getRocletNamespace()
   {
      return rocletNamespace_;
   }
   
   public void setRocletNamespace(boolean rocletNamespace)
   {
      rocletNamespace_ = rocletNamespace;
   }
   
   public RProjectAutoRoxygenizeOptions getAutoRoxygenize()
   {
      return autoRoxygenize_;
   }
   
   private boolean rocletRd_;
   private boolean rocletCollate_;
   private boolean rocletNamespace_;
   private final RProjectAutoRoxygenizeOptions autoRoxygenize_;
}