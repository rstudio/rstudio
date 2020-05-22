/*
 * TextEditingTargetSqlHelper.java
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.sql.model.SqlServerOperations;
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
   void initialize(GlobalDisplay display,
                   SqlServerOperations server)
   {
      display_ = display;
      server_ = server;
   }
   
   
   public boolean previewSql(EditingTarget editingTarget)
   {
      TextEditingTargetCommentHeaderHelper previewSource = new TextEditingTargetCommentHeaderHelper(
         docDisplay_.getCode(),
         "preview",
         "--"
      );
      
      if (!previewSource.hasCommentHeader())
         return false;

      if (previewSource.getFunction().length() == 0)
      {
         previewSource.setFunction(".rs.previewSql");
      }

      previewSource.buildCommand(
         editingTarget.getPath(),
         new OperationWithInput<String>() 
         {
            @Override
            public void execute(String command)
            {
               server_.previewSql(command, new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String message)
                  {
                     if (!StringUtil.isNullOrEmpty(message))
                     {
                        display_.showErrorMessage(
                              "Error Previewing SQL",
                              message);
                     }
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
            }
         }
      );
      
      return true;
   }
   
   private DocDisplay docDisplay_;
   
   private GlobalDisplay display_;
   private SqlServerOperations server_;
}
