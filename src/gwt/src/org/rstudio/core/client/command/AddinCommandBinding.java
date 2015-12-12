/*
 * AddinCommandBinding.java
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
package org.rstudio.core.client.command;

import com.google.inject.Inject;

import org.rstudio.core.client.command.KeyMap.CommandBinding;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;

public class AddinCommandBinding implements CommandBinding
{
   public AddinCommandBinding(String id)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      id_ = id;
   }
   
   @Inject
   private void initialize(AddinsServerOperations server)
   {
      server_ = server;
   }
   
   @Override
   public String getId()
   {
      return id_;
   }

   @Override
   public void execute()
   {
      server_.executeRAddin(id_, new VoidServerRequestCallback());
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
   
   private final String id_;
   private AddinsServerOperations server_;
}
