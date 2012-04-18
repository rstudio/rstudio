/*
 * CodeSearchSuggestion.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.codesearch.model.RFileItem;
import org.rstudio.studio.client.workbench.codesearch.model.RS4MethodParam;
import org.rstudio.studio.client.workbench.codesearch.model.RSourceItem;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

class CodeSearchSuggestion implements Suggestion
{
   public CodeSearchSuggestion(RFileItem fileItem)
   {
      isFileTarget_ = true;
      navigationTarget_ = new CodeNavigationTarget(fileItem.getPath());
      matchedString_ = fileItem.getFilename();
            
      // compute display string
      ImageResource image = 
         fileTypeRegistry_.getIconForFilename(fileItem.getFilename());
   
      displayString_ = createDisplayString(image,
                                           RES.styles().fileImage(),
                                           fileItem.getFilename(),
                                           null,
                                           null);   
   }
   
   public CodeSearchSuggestion(RSourceItem sourceItem, FileSystemItem fsContext)
   {
      isFileTarget_ = false;
      navigationTarget_ = sourceItem.toCodeNavigationTarget();
      matchedString_ = sourceItem.getFunctionName();
      
      // compute display string
      ImageResource image = RES.function();
      if (sourceItem.getType() == RSourceItem.METHOD)
         image = RES.method();
      else if (sourceItem.getType() == RSourceItem.CLASS)
         image = RES.cls();
      
      // adjust context for parent context
      String context = sourceItem.getContext();
      if (fsContext != null)
      {   
         String fsContextPath = fsContext.getPath();
         if (!fsContextPath.endsWith("/"))
            fsContextPath = fsContextPath + "/";
         
         if (context.startsWith(fsContextPath) &&
             (context.length() > fsContextPath.length()))
         {
            context = context.substring(fsContextPath.length());
         }
      }
      
      // create display string
      displayString_ = createDisplayString(image, 
                                           RES.styles().itemImage(),
                                           sourceItem.getFunctionName(),
                                           sourceItem.getSignature(),
                                           context);
   }
   
   public String getMatchedString()
   {
      return matchedString_;
   }

   @Override
   public String getDisplayString()
   {
      return displayString_;
   }
   
   public void setFileDisplayString(String file, String displayString)
   {
      // compute display string
      ImageResource image =  fileTypeRegistry_.getIconForFilename(file);
      displayString_ = createDisplayString(image,
                                           RES.styles().fileImage(),
                                           displayString,
                                           null,
                                           null);   
      
   }

   @Override
   public String getReplacementString()
   {
      return "" ;
   }
   
   public CodeNavigationTarget getNavigationTarget()
   {
      return navigationTarget_;
   }
   
   public boolean isFileTarget()
   {
      return isFileTarget_;
   }
   
   private String createDisplayString(ImageResource image, 
                                      String imageStyle,
                                      String name, 
                                      JsArray<RS4MethodParam> signature,
                                      String context)
   {    
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      SafeHtmlUtil.appendImage(sb, imageStyle, image);
      SafeHtmlUtil.appendSpan(sb, RES.styles().itemName(), name);    
      
      // check for signature
      if (signature != null && signature.length() > 0)
      {
         StringBuilder sigBuilder = new StringBuilder();
         sigBuilder.append("{");
         for (int i=0; i<signature.length(); i++)
         {
            if (i>0)
               sigBuilder.append(", ");
            sigBuilder.append(signature.get(i).getType());
         }
         sigBuilder.append("}");
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.styles().itemName(), 
                                 sigBuilder.toString());
      }
      
      // check for context
      if (context != null)
      {
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.styles().itemContext(),
                                 "(" + context + ")");
      }
      return sb.toSafeHtml().asString();
   }
   
   
   private final boolean isFileTarget_;
   private final CodeNavigationTarget navigationTarget_ ;
   private final String matchedString_;
   private String displayString_;
   private static final FileTypeRegistry fileTypeRegistry_ =
                              RStudioGinjector.INSTANCE.getFileTypeRegistry();
   private static final CodeSearchResources RES = CodeSearchResources.INSTANCE;
}