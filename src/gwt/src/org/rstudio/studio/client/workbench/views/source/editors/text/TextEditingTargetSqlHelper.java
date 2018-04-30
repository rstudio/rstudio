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
      if (sqlPreview != null && sqlPreview.pkg.equals("DBI"))
      {
         server_.getMinimalSourcePath(
            editingTarget.getPath(), 
            new SimpleRequestCallback<String>() {
               @Override
               public void onResponseReceived(String path)
               {
                  String command = ".rs.previewSqlQuery()";
                  eventBus_.fireEvent(new SendToConsoleEvent(command, true));
               }
            });
      }
   }
   
   private SqlPreview parseSqlPreview()
   {
      Iterable<String> lines = StringUtil.getLineIterator(docDisplay_.getCode());
      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (line.startsWith("//"))
         {
            Match match = sqlPreviewPattern_.match(line, 0);
            if (match != null)
            {
               if (match.hasGroup(1) && match.hasGroup(2))
                  return new SqlPreview(match.getGroup(1), match.getGroup(2));               
            }
            
         }   
      }
      
      return null;
   }
   
   
   private class SqlPreview 
   {
      public SqlPreview(String pkg, String args)
      {
         this.pkg = pkg;
         this.args = args;
      }
      
      public final String pkg;
      public final String args;
   }
   
   private static final Pattern sqlPreviewPattern_ = 
         Pattern.create("^//\\s*!preview\\s+(\\w+)(.*)$");
   
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
   private SourceServerOperations server_;
}
