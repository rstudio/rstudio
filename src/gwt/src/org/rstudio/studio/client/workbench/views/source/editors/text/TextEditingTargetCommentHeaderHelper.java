/*
 * TextEditingTargetCommentHeaderHelper.java
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

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
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
         argsString += StringUtil.join(commentHeader_.args, ", ");
      }

      if (!argsString.contains(".file") && !argsString.contains(".code"))
      {
         argsString = StringUtil.ensureQuoted(path) + ", " + argsString;
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
               if (command.contains(".file") || command.contains(".code"))
               {
                  server_.replaceCommentHeader(
                     command,
                     path,
                     code_,
                     new SimpleRequestCallback<String>() {
                        @Override
                        public void onResponseReceived(String replacedCommand)
                        {
                           operation.execute(replacedCommand);
                        }
                     }
                  );
               }
               else
               {
                  operation.execute(command);
               }
            }
         });
   }

   public TextEditingTargetCommentHeaderHelper(String code, String keyword, String comment)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      customHeaderPattern_ = Pattern.create(
         "^" + comment + "\\s*!" + keyword + "\\s*([.a-zA-Z]+[.a-zA-Z0-9:_]*)?\\s*(\\s+|\\(|$)(.*)$",
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
         else if (line.startsWith(comment))
         {
            Match match = customHeaderPattern_.match(line, 0);
            if (match != null &&
                match.hasGroup(2) &&
                match.hasGroup(3))
            {
               String function = match.hasGroup(1) ? match.getGroup(1) : "";
               String options = match.getGroup(3);
               String parenthesis = match.hasGroup(2) ? match.getGroup(2) : "";

               // Support for explicit function calls
               if (options.startsWith("(")) {
                  options = options.replaceAll("^\\(|\\)$", "");
               }
               else if (parenthesis.startsWith("(")) {
                  options = options.replaceAll("\\)$", "");
               }

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

   public String getFunction()
   {
      return commentHeader_.function;
   }

   public void setFunction(String name)
   {
      commentHeader_.function = name;
   }

   private class CommentHeader
   {
      public CommentHeader(String function, String[] args)
      {
         this.function = function;
         this.args = args;
      }
      
      public String function;
      public String[] args;
   }
   
   public boolean hasCommentHeader()
   {
      return commentHeader_ != null;
   }
   
   private Pattern customHeaderPattern_;
   private CommentHeader commentHeader_ = null;
   private String code_;
   private SourceServerOperations server_;
}
