/*
 * TextEditingTargetSqlHelper.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;

import com.google.inject.Inject;

public class TextEditingTargetSqlHelper
{
   public TextEditingTargetSqlHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }
   
   
   public void previewSql(EditingTarget editingTarget)
   {
      TextEditingTargetCommentHeaderHelper previewSource = new TextEditingTargetCommentHeaderHelper(
         docDisplay_.getCode(),
         "preview",
         "--"
      );

      if (previewSource.getFunction().length() == 0)
      {
         previewSource.setFunction("previewSql");
      }

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
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
}
