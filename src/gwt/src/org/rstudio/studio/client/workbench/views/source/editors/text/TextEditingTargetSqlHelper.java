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
      SqlPreview sqlPreview = parseSqlPreview();
      if (sqlPreview != null)
      {
         String statement = "";
         
         if (editingTarget.dirtyState().getValue() || editingTarget.getPath() == null)
         {
            statement = docDisplay_.getCode();
         }
         else
         {
            statement = editingTarget.getPath();
         }

         String statementEscaped = statement.replaceAll("\\\"", "\\\\\"");
         String command = "previewSql(statement = \"" + statementEscaped + "\"," +
            sqlPreview.args +
            ")";

         eventBus_.fireEvent(new SendToConsoleEvent(command, true));
      }
   }
   
   private SqlPreview parseSqlPreview()
   {
      String args = "";
      Boolean hasPreview = false;

      Iterable<String> lines = StringUtil.getLineIterator(docDisplay_.getCode());

      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (!hasPreview && line.startsWith("--"))
         {
            Match match = sqlPreviewPattern_.match(line, 0);
            if (match != null)
            {
               if (match.hasGroup(1)) {
                  hasPreview = true;
                  args = match.getGroup(1);
               }
            }
         }   
      }
      
      if (hasPreview) {
         return new SqlPreview(args);
      } else {
         return null;
      }
   }
   
   
   private class SqlPreview 
   {
      public SqlPreview(String args)
      {
         this.args = args;
      }
      
      public final String args;
   }
   
   private static final Pattern sqlPreviewPattern_ = 
         Pattern.create("^--\\s*!preview\\s+(.*)$");
   
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
}
