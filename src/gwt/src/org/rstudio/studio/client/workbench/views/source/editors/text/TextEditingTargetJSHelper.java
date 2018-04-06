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

public class TextEditingTargetJSHelper
{
   public TextEditingTargetJSHelper(DocDisplay docDisplay)
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
   
   
   public void previewJS(EditingTarget editingTarget)
   {
      JsPreview jsPreview = parseJSPreview();
      if (jsPreview != null && jsPreview.pkg.equals("r2d3"))
      {
         server_.getMinimalSourcePath(
            editingTarget.getPath(), 
            new SimpleRequestCallback<String>() {
               @Override
               public void onResponseReceived(String path)
               {
                  String command = "r2d3::r2d3(" + jsPreview.args + 
                        ", script=\"" + path + "\")";
                  eventBus_.fireEvent(new SendToConsoleEvent(command, true));
               }
            });
      }
   }
   
   private JsPreview parseJSPreview()
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
            Match match = jsPreviewPattern_.match(line, 0);
            if (match != null)
            {
               if (match.hasGroup(1) && match.hasGroup(2))
                  return new JsPreview(match.getGroup(1), match.getGroup(2));               
            }
            
         }   
      }
      
      return null;
   }
   
   
   private class JsPreview 
   {
      public JsPreview(String pkg, String args)
      {
         this.pkg = pkg;
         this.args = args;
      }
      
      public final String pkg;
      public final String args;
   }
   
   private static final Pattern jsPreviewPattern_ = 
         Pattern.create("^//\\s*!preview\\s+(\\w+) (.*)$");
   
  
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
   private SourceServerOperations server_;
  
}
