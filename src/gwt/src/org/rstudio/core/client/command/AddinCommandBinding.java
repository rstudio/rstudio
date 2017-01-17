/*
 * AddinCommandBinding.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.core.client.command;

import org.rstudio.core.client.command.AppCommand.Context;
import org.rstudio.core.client.command.KeyMap.CommandBinding;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;

public class AddinCommandBinding implements CommandBinding
{
   public AddinCommandBinding(RAddin addin)
   {
      addin_ = addin;
      executor_ = new AddinExecutor();
   }
   
   @Override
   public String getId()
   {
      return addin_.getId();
   }

   @Override
   public void execute()
   {
      executor_.execute(addin_);
   }

   @Override
   public boolean isEnabled()
   {
      return true;
   }
   
   @Override
   public boolean isUserDefinedBinding()
   {
      return true;
   }
   
   @Override
   public Context getContext()
   {
      return AppCommand.Context.Addin;
   }

   private final RAddin addin_;
   private final AddinExecutor executor_;
}
