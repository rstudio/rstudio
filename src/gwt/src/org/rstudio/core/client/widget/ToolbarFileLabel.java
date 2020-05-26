/*
 * ToolbarFileLabel.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.ui.Image;
import org.rstudio.studio.client.common.filetypes.FileIcon;

public class ToolbarFileLabel
{
   public ToolbarFileLabel(Toolbar toolbar, int maxNameWidth)
   {
      this(toolbar, maxNameWidth, false);
   }
   
   public ToolbarFileLabel(Toolbar toolbar, 
                           int maxNameWidth, 
                           boolean addToRight)
   {
      ThemeStyles styles = ThemeStyles.INSTANCE;
      maxNameWidth_ = maxNameWidth;
      fileImage_ = new Image();
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(styles.subtitle());
      fileLabel_.addStyleName(styles.toolbarFileLabel());
      
      if (addToRight)
      {
         toolbar.addRightWidget(fileImage_);
         toolbar.addLeftWidget(fileLabel_);
      }
      else
      {
         toolbar.addLeftWidget(fileImage_);
         toolbar.addLeftWidget(fileLabel_);
      }
   }
  
   public void setFileName(String fileName)
   {
      if (StringUtil.isNullOrEmpty(fileName))
      {
         fileImage_.setVisible(false);
         fileLabel_.setText("");
      }
      else
      {
         FileIcon fileIcon = 
               RStudioGinjector.INSTANCE.getFileTypeRegistry().getIconForFilename(fileName);
         fileImage_.setResource(fileIcon.getImageResource());
         fileImage_.setAltText(fileIcon.getDescription());
         fileImage_.setVisible(true);
         
         String shortFileName = StringUtil.shortPathName(
                                    FileSystemItem.createFile(fileName), 
                                    ThemeStyles.INSTANCE.subtitle(), 
                                    maxNameWidth_);
         fileLabel_.setText(shortFileName);
      }
   }
   
   private final int maxNameWidth_;
   private final Image fileImage_;
   private final ToolbarLabel fileLabel_;
}
