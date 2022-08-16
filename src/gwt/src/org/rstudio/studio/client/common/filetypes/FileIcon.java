/*
 * FileIcon.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.StudioClientCommonConstants;

public class FileIcon
{
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);

   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final FileIcon PUBLIC_FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPublicFolder2x()), constants_.publicFolderDesc());

   public static final FileIcon FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconFolder2x()), constants_.folderDesc());

   public static final FileIcon TEXT_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconText2x()), constants_.textFileDesc());

   public static final FileIcon CSV_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconCsv2x()), "CSV");

   public static final FileIcon PDF_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPdf2x()), "PDF");

   public static final FileIcon IMAGE_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconPng2x()), constants_.imageFileDesc());

   public static final FileIcon RDATA_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRdata2x()), "RData");

   public static final FileIcon RDS_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRdata2x()), "RDS");

   public static final FileIcon RPROJECT_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconRproject2x()), "RProject");

   public static final FileIcon PARENT_FOLDER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconUpFolder2x()), constants_.parentFolderDesc());

   public static final FileIcon OBJECT_EXPLORER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconObjectExplorer2x()), constants_.exploreObjectDesc());

   public static final FileIcon CODE_BROWSER_ICON =
         new FileIcon(new ImageResource2x(ICONS.iconSourceViewer2x()), constants_.rSourceViewerDesc());

   public static final FileIcon PROFILER_ICON =
         new FileIcon(new ImageResource2x(FileIconResources.INSTANCE.iconProfiler2x()), constants_.profilerDesc());

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

