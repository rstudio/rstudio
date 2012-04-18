/*
 * ToolbarFileLabel.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.ui.Image;

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
      fileImage_.setResource(RStudioGinjector.INSTANCE.getFileTypeRegistry()
                                                .getIconForFilename(fileName));
      
      String shortFileName = StringUtil.shortPathName(
                                 FileSystemItem.createFile(fileName), 
                                 ThemeStyles.INSTANCE.subtitle(), 
                                 maxNameWidth_);
      fileLabel_.setText(shortFileName);
   }
   
   private final int maxNameWidth_;
   private final Image fileImage_;
   private final ToolbarLabel fileLabel_;
}
