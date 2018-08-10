/*
 * TextEditingTargetRHelper.java
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
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.inject.Inject;

public class TextEditingTargetRHelper
{
   public TextEditingTargetRHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(GlobalDisplay display, EventBus eventBus, SourceServerOperations server)
   {
      display_ = display;
      eventBus_ = eventBus;
      server_ = server;
   }

   public void customSource(EditingTarget editingTarget)
   {
      CustomSource customSource = parseCustomSource();
      if (customSource != null)
      {
         server_.getMinimalSourcePath(
            editingTarget.getPath(), 
            new SimpleRequestCallback<String>() {
               @Override
               public void onResponseReceived(String path)
               {
                  String argsString = "";
                  if (customSource.args.length() > 0)
                  {
                     argsString = ", " + customSource.args;
                  }

                  String command = customSource.function + "(\"" + path + "\"" + argsString + ")";
                  eventBus_.fireEvent(new SendToConsoleEvent(command, true));
               }
            });
      }
   }

   private CustomSource parseCustomSource()
   {
      Iterable<String> lines = StringUtil.getLineIterator(docDisplay_.getCode());
      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (line.startsWith("#"))
         {
            Match match = customSourcePattern_.match(line, 0);
            if (match != null &&
                match.hasGroup(1) &&
                match.hasGroup(2))
            {
               String function = match.getGroup(1);
               String options = match.getGroup(2);
               
               return new CustomSource(function, options);
            }
            
         }   
      }
      
      return null;
   }
   
   private class CustomSource 
   {
      public CustomSource(String function, String args)
      {
         this.function = function;
         this.args = args;
      }
      
      public final String function;
      public final String args;
   }
   
   private static final Pattern customSourcePattern_ = 
         Pattern.create("^#\\s*!source\\s+([.a-zA-Z]+[.a-zA-Z0-9:_]*)\\s*(.*)$", "");
   
   private GlobalDisplay display_;
   private EventBus eventBus_; 
   private DocDisplay docDisplay_;
   private SourceServerOperations server_;
}
