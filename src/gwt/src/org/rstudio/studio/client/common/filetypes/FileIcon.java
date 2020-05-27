/*
 * FileIcon.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.resources.ImageResource2x;

public class FileIcon
{
   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final FileIcon PUBLIC_FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPublicFolder2x()), "Public Folder");

   public static final FileIcon FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconFolder2x()), "Folder");

   public static final FileIcon TEXT_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconText2x()), "Text file");

   public static final FileIcon CSV_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconCsv2x()), "CSV");

   public static final FileIcon PDF_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPdf2x()), "PDF");

   public static final FileIcon IMAGE_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPng2x()), "Image file");

   public static final FileIcon RDATA_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRdata2x()), "RData");

   public static final FileIcon RDS_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRdata2x()), "RDS");

   public static final FileIcon RPROJECT_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRproject2x()), "RProject");

   public static final FileIcon PARENT_FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconUpFolder2x()), "Parent folder");

   public static final FileIcon OBJECT_EXPLORER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconObjectExplorer2x()), "Explore object");

   public static final FileIcon CODE_BROWSER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconSourceViewer2x()), "R source viewer");

   public static final FileIcon PROFILER_ICON =
         new FileIcon(new ImageResource2x(FileIconResources.INSTANCE.iconProfiler2x()), "Profiler");

   public FileIcon(ImageResource imageResource, String description)
   {
      imageResource_ = imageResource;
      description_ = description;
   }

   public ImageResource getImageResource()
   {
      return imageResource_;
   }

   public void setImageResource(ImageResource imageResource)
   {
      imageResource_ = imageResource;
   }

   public String getDescription()
   {
      return description_;
   }

   public void setDescription(String description)
   {
      description_ = description;
   }

   public Image getImage()
   {
      Image icon = new Image(getImageResource());
      icon.setAltText(getDescription());
      return icon;
   }

   private ImageResource imageResource_;
   private String description_;
}

