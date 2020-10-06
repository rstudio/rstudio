/*
 * CodeSearchSuggestion.java
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
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.resources.ImageResource2x;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.XRef;
import org.rstudio.core.client.container.SafeMap;
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
         fileTypeRegistry_.getIconForFilename(fileItem.getFilename()).getImageResource();
   
      displayString_ = createDisplayString(
            image, RES.styles().fileImage(),
            fileItem.getFilename(), RES.styles().fileItem(),
            null, null,
            null, null);
   }
   
   private ImageResource iconForXRef(XRef xref)
   {
      String type = xref.getType();
      if (XREF_ICON_MAP.containsKey(type))
         return new ImageResource2x(XREF_ICON_MAP.get(type));
      
      return null;
   }
   
   private ImageResource iconForSourceItem(SourceItem sourceItem)
   {
      // check for bookdown xref
      if (sourceItem.hasXRef())
      {
         XRef xref = sourceItem.getXRef();
         ImageResource icon = iconForXRef(xref);
         if (icon != null)
            return icon;
      }
      
      // compute image
      ImageResource image = null;
      switch (sourceItem.getType())
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
      case SourceItem.SECTION:
         image = new ImageResource2x(CodeIcons.INSTANCE.section2x());
         break;
      case SourceItem.NONE:
      default:
         image = new ImageResource2x(CodeIcons.INSTANCE.keyword2x());
         break;
      }
      
      return image;
   }
   
   public CodeSearchSuggestion(SourceItem sourceItem, FileSystemItem fsContext)
   {
      isFileTarget_ = false;
      navigationTarget_ = sourceItem.toCodeNavigationTarget();
      matchedString_ = sourceItem.getName();
      
      // get icon
      ImageResource image = iconForSourceItem(sourceItem);
      
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
      
      if (sourceItem.hasXRef())
      {
         displayString_ = createDisplayString(
               image, RES.styles().xrefImage(),
               name, RES.styles().xrefItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().itemContext());
      }
      else if (image.getHeight() < 16)
      {
         displayString_ = createDisplayString(
               image, RES.styles().smallCodeImage(),
               name, RES.styles().smallCodeItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().smallItemContext());
      }
      else
      {
         displayString_ = createDisplayString(
               image, RES.styles().codeImage(),
               name, RES.styles().codeItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().itemContext());
      }
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
      ImageResource image =
            fileTypeRegistry_.getIconForFilename(file).getImageResource();
      
      displayString_ = createDisplayString(
            image, RES.styles().fileImage(),
            displayString, RES.styles().fileItem(),
            null, null,
            null, null);
   }

   @Override
   public String getReplacementString()
   {
      return "";
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
                                      String nameStyle,
                                      String extraInfo,
                                      String extraInfoStyle,
                                      String context,
                                      String contextStyle)
   {    
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      
      sb.append(SafeHtmlUtil.createOpenTag("div", "class", imageStyle));
      sb.append(SafeHtmlUtil.createOpenTag("img",
            "src", image.getSafeUri().asString(),
            "width", Integer.toString(image.getWidth()),
            "height", Integer.toString(image.getHeight())));
      sb.appendHtmlConstant("</img>");
      sb.appendHtmlConstant("</div>");
      
      SafeHtmlUtil.appendSpan(sb, nameStyle, name);
      
      // check for extra info
      if (!StringUtil.isNullOrEmpty(extraInfo))
         SafeHtmlUtil.appendSpan(sb, extraInfoStyle, extraInfo);
      
      // check for context
      if (context != null)
         SafeHtmlUtil.appendSpan(sb, contextStyle, "(" + context + ")");
      
      return sb.toSafeHtml().asString();
   }
   
   private static final SafeMap<String, ImageResource> createXRefIconMap()
   {
      SafeMap<String, ImageResource> map = new SafeMap<>();
      
      // section headers
      map.put("h1", CodeIcons.INSTANCE.sectionH12x());
      map.put("h2", CodeIcons.INSTANCE.sectionH22x());
      map.put("h3", CodeIcons.INSTANCE.sectionH32x());
      map.put("h4", CodeIcons.INSTANCE.sectionH42x());
      map.put("h5", CodeIcons.INSTANCE.sectionH52x());
      map.put("h6", CodeIcons.INSTANCE.sectionH62x());
      
      // figures
      map.put("fig", CodeIcons.INSTANCE.figure2x());
      
      // tables
      map.put("tab", CodeIcons.INSTANCE.table2x());
      
      // math-related sections (e.g. theorems)
      map.put("thm", CodeIcons.INSTANCE.function2x());
      map.put("lem", CodeIcons.INSTANCE.function2x());
      map.put("cor", CodeIcons.INSTANCE.function2x());
      map.put("prp", CodeIcons.INSTANCE.function2x());
      map.put("cnj", CodeIcons.INSTANCE.function2x());
      map.put("def", CodeIcons.INSTANCE.function2x());
      map.put("exr", CodeIcons.INSTANCE.function2x());
      map.put("eq",  CodeIcons.INSTANCE.function2x());
      
      return map;
      
   }
   
   private final boolean isFileTarget_;
   private final CodeNavigationTarget navigationTarget_;
   private final String matchedString_;
   private String displayString_;
   private static final FileTypeRegistry fileTypeRegistry_ =
                              RStudioGinjector.INSTANCE.getFileTypeRegistry();
   private static final CodeSearchResources RES = CodeSearchResources.INSTANCE;
   private static final SafeMap<String, ImageResource> XREF_ICON_MAP = createXRefIconMap();
}
