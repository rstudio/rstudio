/*
 * TextEditingTargetSqlHelper.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.PreviewResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.sql.model.SqlServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import com.google.inject.Inject;
import com.google.gwt.core.client.GWT;

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
               doPreviewSql(command, false);
            }
         }
      );

      return true;
   }

   // Run the 'preview_sql' RPC. When the connection expression in the SQL
   // header is not statically safe, the backend declines to evaluate it and
   // returns a "confirm" result; we then ask the user before retrying with
   // allowUnsafe = true.
   private void doPreviewSql(final String command, boolean allowUnsafe)
   {
      server_.previewSql(command, allowUnsafe, new ServerRequestCallback<PreviewResult>()
      {
         @Override
         public void onResponseReceived(PreviewResult result)
         {
            if (result == null || result.isOk())
               return;

            if (result.isConfirm())
            {
               display_.showYesNoMessage(
                     MessageDisplay.MSG_WARNING,
                     constants_.previewRunCodeConfirmCaption(),
                     constants_.previewRunCodeConfirmMessage(result.getExpression()),
                     new Operation()
                     {
                        @Override
                        public void execute()
                        {
                           doPreviewSql(command, true);
                        }
                     },
                     false);
               return;
            }

            if (!StringUtil.isNullOrEmpty(result.getMessage()))
            {
               display_.showErrorMessage(
                     constants_.errorPreviewingSql(),
                     result.getMessage());
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private DocDisplay docDisplay_;
   
   private GlobalDisplay display_;
   private SqlServerOperations server_;
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
