/*
 * BuildToolsRoxygenOptions.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
   
   public boolean getRocletRd()
   {
      return rocletRd_;
   }
   
   public boolean getRocletCollate()
   {
      return rocletCollate_;
   }
   
   public boolean getRocletNamespace()
   {
      return rocletNamespace_;
   }
   
   public RProjectAutoRoxygenizeOptions getAutoRoxygenize()
   {
      return autoRoxygenize_;
   }
   
   private final boolean rocletRd_;
   private final boolean rocletCollate_;
   private final boolean rocletNamespace_;
   private final RProjectAutoRoxygenizeOptions autoRoxygenize_;
}