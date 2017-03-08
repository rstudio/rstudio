/*
 * CodeSearchSuggestion.java
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
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
import org.rstudio.studio.client.workbench.codesearch.model.FileItem;
import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

class CodeSearchSuggestion implements Suggestion
{
   public CodeSearchSuggestion(FileItem fileItem)
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
   
   public CodeSearchSuggestion(SourceItem sourceItem, FileSystemItem fsContext)
   {
      isFileTarget_ = false;
      navigationTarget_ = sourceItem.toCodeNavigationTarget();
      matchedString_ = sourceItem.getName();
      
      // compute image
      ImageResource image = null;
      switch(sourceItem.getType())
      {
      case SourceItem.FUNCTION:
         image = new ImageResource2x(StandardIcons.INSTANCE.functionLetter2x());
         break;
      case SourceItem.METHOD:
         image = new ImageResource2x(StandardIcons.INSTANCE.methodLetter2x());
         break;
      case SourceItem.CLASS:
         image = new ImageResource2x(CodeIcons.INSTANCE.clazz2x());
         break;
      case SourceItem.ENUM:
         image = new ImageResource2x(CodeIcons.INSTANCE.enumType2x());
         break;
      case SourceItem.ENUM_VALUE:
         image = new ImageResource2x(CodeIcons.INSTANCE.enumValue2x());
         break;
      case SourceItem.NAMESPACE:
         image = new ImageResource2x(CodeIcons.INSTANCE.namespace2x());
         break;
      case SourceItem.NONE:
      default:
         image = new ImageResource2x(CodeIcons.INSTANCE.keyword2x());
         break;
      }
      
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
      
      // resolve name (include parent if there is one)
      String name = sourceItem.getName();
      if (!StringUtil.isNullOrEmpty(sourceItem.getParentName()))
         name = sourceItem.getParentName() + "::" + name;
      
      // create display string
      displayString_ = createDisplayString(image, 
                                           RES.styles().itemImage(),
                                           name,
                                           sourceItem.getExtraInfo(),
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
                                      String extraInfo,
                                      String context)
   {    
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      SafeHtmlUtil.appendImage(sb, imageStyle, image);
      SafeHtmlUtil.appendSpan(sb, RES.styles().itemName(), name);    
      
      // check for extra info
      if (!StringUtil.isNullOrEmpty(extraInfo))
      {
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.styles().itemName(), 
                                 extraInfo);
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