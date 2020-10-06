/*
 * EditableFileType.java
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

public abstract class EditableFileType extends FileType
{
   public EditableFileType(String id, String label, ImageResource defaultIcon)
   {
      super(id);
      label_ = label;
      defaultIcon_ = defaultIcon;
   }

   public String getLabel()
   {
      return label_;
   }

   public ImageResource getDefaultIcon()
   {
      return defaultIcon_;
   }

   @Override
   public FileIcon getDefaultFileIcon()
   {
      return new FileIcon(defaultIcon_, label_);
   }

   private final String label_;
   private final ImageResource defaultIcon_;
}
