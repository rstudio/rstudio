/*
 * FileIconResourceCell.java
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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class FileIconResourceCell extends AbstractCell<FileIcon>
{
   private static FileIconRenderer renderer;

   /**
    * Construct a new ImageResourceCell.
    */
   public FileIconResourceCell()
   {
      if (renderer == null)
      {
         renderer = new FileIconRenderer();
      }
   }

   @Override
   public void render(Context context, FileIcon value, SafeHtmlBuilder sb)
   {
      if (value != null)
      {
         sb.append(renderer.render(value));
      }
   }
}
