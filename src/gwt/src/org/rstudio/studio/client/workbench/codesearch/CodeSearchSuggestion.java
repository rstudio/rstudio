/*
 * CodeSearchSuggestion.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileIcon;
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
      isXRef_ = false;
      matchedString_ = fileItem.getFilename();
      nameMatchedString_ = "";
            
      // compute display string
      ImageResource image = 
         fileTypeRegistry_.getIconForFilename(fileItem.getFilename()).getImageResource();
   
      // decorative: the file name's extension already conveys its type
      displayString_ = createDisplayString(
            image, "", RES.styles().fileImage(),
            fileItem.getFilename(), RES.styles().fileItem(),
            null, null,
            null, null);
   }
   
   private ImageResource iconForSourceItem(SourceItem sourceItem)
   {
      // check for bookdown xref
      if (sourceItem.hasXRef())
      {
         ImageResource icon = CodeSearchItemKind.xrefIcon(sourceItem.getXRef().getType());
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
      case SourceItem.TEST:
         image = new ImageResource2x(CodeIcons.INSTANCE.test2x());
         break;
      case SourceItem.ROXYGEN:
         image = new ImageResource2x(CodeIcons.INSTANCE.roxygen2x());
         break;
      case SourceItem.MACRO:
         image = new ImageResource2x(CodeIcons.INSTANCE.macro2x());
         break;
      case SourceItem.PACKAGE:
         image = new ImageResource2x(CodeIcons.INSTANCE.rPackage2x());
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
      isXRef_ = sourceItem.hasXRef();
      matchedString_ = sourceItem.getName();

      // the server sends a cross-reference's name (e.g. a theorem's) as its
      // extra info, and finds the cross-reference by that name too
      nameMatchedString_ = sourceItem.hasXRef()
            ? StringUtil.notNull(sourceItem.getExtraInfo())
            : "";
      
      // get icon
      ImageResource image = iconForSourceItem(sourceItem);
      String altText = CodeSearchItemKind.label(
            sourceItem.getType(),
            sourceItem.hasXRef() ? sourceItem.getXRef().getType() : null);
      
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
            context = StringUtil.substring(context, fsContextPath.length());
         }
      }
      
      // resolve name (include parent if there is one)
      String name = sourceItem.getName();

      // remove "t " for test items that was artifically added by testThatCallIndexer()
      if (sourceItem.getType() == SourceItem.TEST)
         name = name.replaceFirst("t ", "");
         
      if (!StringUtil.isNullOrEmpty(sourceItem.getParentName()))
         name = sourceItem.getParentName() + "::" + name;
      
      if (sourceItem.hasXRef())
      {
         displayString_ = createDisplayString(
               image, altText, RES.styles().xrefImage(),
               name, RES.styles().xrefItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().itemContext());
      }
      else if (image.getHeight() < 16)
      {
         displayString_ = createDisplayString(
               image, altText, RES.styles().smallCodeImage(),
               name, RES.styles().smallCodeItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().smallItemContext());
      }
      else
      {
         displayString_ = createDisplayString(
               image, altText, RES.styles().codeImage(),
               name, RES.styles().codeItem(),
               sourceItem.getExtraInfo(), RES.styles().itemContext(),
               context, RES.styles().itemContext());
      }
   }
   
   public String getMatchedString()
   {
      return matchedString_;
   }

   // a second string the query can match, or "" if there is none
   public String getNameMatchedString()
   {
      return nameMatchedString_;
   }

   @Override
   public String getDisplayString()
   {
      return displayString_;
   }
   
   public void setFileDisplayString(String file, String displayString)
   {
      String fileName = FileSystemItem.createFile(file).getName();
      FileIcon icon = fileTypeRegistry_.getIconForFilename(fileName);
      ImageResource image = icon.getImageResource();
      
      // decorative: the file name's extension already conveys its type
      displayString_ = createDisplayString(
            image, "", RES.styles().fileImage(),
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

   public boolean isXRef()
   {
      return isXRef_;
   }
   
   private String createDisplayString(ImageResource image,
                                      String altText,
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
            "alt", altText,
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
   
   private final boolean isFileTarget_;
   private final boolean isXRef_;
   private final CodeNavigationTarget navigationTarget_;
   private final String matchedString_;
   private final String nameMatchedString_;
   private String displayString_;
   private static final FileTypeRegistry fileTypeRegistry_ =
                              RStudioGinjector.INSTANCE.getFileTypeRegistry();
   private static final CodeSearchResources RES = CodeSearchResources.INSTANCE;
}
