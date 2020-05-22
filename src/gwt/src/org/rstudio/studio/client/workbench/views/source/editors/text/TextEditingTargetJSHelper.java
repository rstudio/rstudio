/*
 * TextEditingTargetJSHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
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
   void initialize(GlobalDisplay display, EventBus eventBus)
   {
      display_ = display;
      eventBus_ = eventBus;
   }
   
   
   public boolean previewJS(EditingTarget editingTarget)
   {
      TextEditingTargetCommentHeaderHelper previewSource = new TextEditingTargetCommentHeaderHelper(
         docDisplay_.getCode(),
         "preview",
         "//"
      );
      
      if (!previewSource.hasCommentHeader())
         return false;

      if (!previewSource.getFunction().equals("r2d3"))
      {
         display_.showErrorMessage(
                        "Error Previewing JavaScript",
                        "'" + previewSource.getFunction() + "' is not a known previewer for " +
                        "JavaScript files. Did you mean 'r2d3'?");
      }
      else
      {
         previewSource.setFunction("r2d3::r2d3");

         previewSource.buildCommand(
            editingTarget.getPath(),
            new OperationWithInput<String>() 
            {
               @Override
               public void execute(String command)
               {
                  eventBus_.fireEvent(new SendToConsoleEvent(command, true));
               }
            }
         );
      }
      
      return true;
   }
   
   private GlobalDisplay display_;
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
  
}
