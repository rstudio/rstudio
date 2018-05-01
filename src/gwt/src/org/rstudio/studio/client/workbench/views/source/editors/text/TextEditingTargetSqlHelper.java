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
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.inject.Inject;

public class TextEditingTargetSqlHelper
{
   public TextEditingTargetSqlHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(EventBus eventBus, SourceServerOperations server)
   {
      eventBus_ = eventBus;
      server_ = server;
   }
   
   
   public void previewSql(EditingTarget editingTarget)
   {
      SqlPreview sqlPreview = parseSqlPreview();
      if (sqlPreview != null && sqlPreview.fn.equals("dbGetQuery"))
      {
         String command = ".rs.previewSql(\"" + editingTarget.getPath() + "\"," +
            sqlPreview.args +
            ")";

         eventBus_.fireEvent(new SendToConsoleEvent(command, true));
      }
   }
   
   private SqlPreview parseSqlPreview()
   {
      String code = "";
      String fn = "";
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
               if (match.hasGroup(1) && match.hasGroup(2)) {
                  hasPreview = true;
                  fn = match.getGroup(1);
                  args = match.getGroup(2);
               }
            }
         }   
         else
         {
            code += line + " ";
         }
      }
      
      if (hasPreview) {
         return new SqlPreview(fn, args, code);
      } else {
         return null;
      }
   }
   
   
   private class SqlPreview 
   {
      public SqlPreview(String fn, String args, String code)
      {
         this.fn = fn;
         this.args = args;
         this.code = code;
      }
      
      public final String fn;
      public final String args;
      public final String code;
   }
   
   private static final Pattern sqlPreviewPattern_ = 
         Pattern.create("^--\\s*!preview\\s+(\\w+)(.*)$");
   
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
   private SourceServerOperations server_;
}
