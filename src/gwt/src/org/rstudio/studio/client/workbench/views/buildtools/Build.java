/*
 * Build.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.inject.Inject;

import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public class Build extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
     
   }
   
   @Inject
   public Build(Display display, 
                GlobalDisplay globalDisplay)
   {
      super(display);
      display_ = display;
      
      globalDisplay_ = globalDisplay;
     
   }
  
   void onBuildAll()
   {
      globalDisplay_.showErrorMessage("Build Package", "Build Package");
   }
   
   void onCheckPackage()
   {
      globalDisplay_.showErrorMessage("Check Package", "Check Package");
   }
   
   
   void onRebuildAll()
   {
   }
   
   void onCleanAll()
   {
   }
   
  
   private final GlobalDisplay globalDisplay_;
   
   @SuppressWarnings("unused")
   private final Display display_ ;

   
}
