/*
 * TextEditingTargetCommentHeaderHelper.java
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

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.inject.Inject;

public class TextEditingTargetCommentHeaderHelper
{
   @Inject
   void initialize(SourceServerOperations server)
   {
      server_ = server;
   }

   private String buildCommand(String path)
   {
      String argsString = "";
      
      if (commentHeader_.args.length > 0)
      {
         boolean implicitPath = true;

         for (int idxOpt = 0; idxOpt < commentHeader_.args.length; idxOpt++)
         {
            if (commentHeader_.args[idxOpt].equals(".file"))
            {
               implicitPath = false;
               commentHeader_.args[idxOpt] = StringUtil.ensureQuoted(path);
            }
            else if (commentHeader_.args[idxOpt].equals(".code"))
            {
               implicitPath = false;
               String quotedCode = code_.replaceAll("\\\\", "\\\\\\\\");
               commentHeader_.args[idxOpt] = StringUtil.ensureQuoted(quotedCode);
            }
         }

         if (implicitPath)
         {
            argsString = StringUtil.ensureQuoted(path) + ", ";
         }

         argsString += StringUtil.join(commentHeader_.args, ", ");
      }

      return commentHeader_.function + "(" + argsString + ")";
   }

   public void buildCommand(String path, OperationWithInput<String> operation)
   {
      server_.getMinimalSourcePath(
         path, 
         new SimpleRequestCallback<String>() {
            @Override
            public void onResponseReceived(String path)
            {
               String command = buildCommand(path);
               operation.execute(command);
            }
         });
   }

   public TextEditingTargetCommentHeaderHelper(String code, String keyword)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      customHeaderPattern_ = Pattern.create(
         "^#\\s*!" + keyword + "\\s+([.a-zA-Z]+[.a-zA-Z0-9:_]*)\\s*(.*)$",
         ""
      );

      code_ = code;
      Iterable<String> lines = StringUtil.getLineIterator(code);
      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (line.startsWith("#"))
         {
            Match match = customHeaderPattern_.match(line, 0);
            if (match != null &&
                match.hasGroup(1) &&
                match.hasGroup(2))
            {
               String function = match.getGroup(1);
               String options = match.getGroup(2);

               // Support for explicit function calls
               options = options.replaceAll("^\\(|\\)$", "");

               // Split parameters
               String[] optionsArr = new String[] {};
               if (options.trim().length() > 0)
               {
                  optionsArr = JsUtil.toStringArray(StringUtil.split(options, ","));
                  for (int idxOpt = 0; idxOpt < optionsArr.length; idxOpt++)
                  {
                     optionsArr[idxOpt] = optionsArr[idxOpt].trim();
                  }
               }
               
               commentHeader_ = new CommentHeader(function, optionsArr);
            }
            
         }   
      }
   }

   private boolean hasHeader()
   {
      return commentHeader_ != null;
   }
   
   private class CommentHeader
   {
      public CommentHeader(String function, String[] args)
      {
         this.function = function;
         this.args = args;
      }
      
      public final String function;
      public final String[] args;
   }
   
   private Pattern customHeaderPattern_;
   private CommentHeader commentHeader_ = null;
   private String code_;
   private SourceServerOperations server_;
}
