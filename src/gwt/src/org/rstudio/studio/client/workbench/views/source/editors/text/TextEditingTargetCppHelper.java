/*
 * TextEditingTargetCppHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;

public class TextEditingTargetCppHelper
{
   public TextEditingTargetCppHelper(BuildServerOperations server)
   {
      server_ = server;
   }
   
   public void checkBuildCppDependencies(final WarningBarDisplay display, 
                                         TextFileType fileType)
   {
      // bail if this isn't a C file or we've already verified we can build
      if (!fileType.isC() || canBuildCpp_)
         return;
      
      server_.canBuildCpp(new ServerRequestCallback<Boolean>() {
         
         @Override
         public void onResponseReceived(Boolean canBuild)
         {
            if (canBuild)
            {
               canBuildCpp_ = true;
            }
            else 
            {
               display.showWarningBar(
                  "The tools required to build C/C++ code for R " +
                  "are not currently installed");
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            // ignore error since this is merely advisory
         }
      });
   }
      
  
   private BuildServerOperations server_;
  
   
   // cache the canBuildCpp value statically -- once we get an affirmative
   // response we never check again
   private static boolean canBuildCpp_ = false;
}
