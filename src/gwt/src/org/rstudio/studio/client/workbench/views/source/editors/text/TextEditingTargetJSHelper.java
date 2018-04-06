/*
 * TextEditingTargetJSHelper.java
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

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;

import com.google.inject.Inject;

public class TextEditingTargetJSHelper
{
   public TextEditingTargetJSHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay, ConsoleDispatcher consoleDispatcher)
   {
      globalDisplay_ = globalDisplay;
      consoleDispatcher_ = consoleDispatcher;
   }
   
   public void previewJS(EditingTarget editingTarget)
   {
      globalDisplay_.showErrorMessage("Preview JS", editingTarget.getPath());
   }
   
   
   @SuppressWarnings("unused")
   private ConsoleDispatcher consoleDispatcher_;
   private GlobalDisplay globalDisplay_;
   
   private DocDisplay docDisplay_;
  
}
