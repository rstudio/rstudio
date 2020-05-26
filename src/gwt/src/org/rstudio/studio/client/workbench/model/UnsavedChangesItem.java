/*
 * UnsavedChangesItem.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.studio.client.common.filetypes.FileIcon;

public class UnsavedChangesItem extends JavaScriptObject
   implements UnsavedChangesTarget
{
   protected UnsavedChangesItem()
   {
   }
   
   public final static UnsavedChangesItem create(UnsavedChangesTarget target)
   {
      return create(target.getId(), target.getIcon(), target.getTitle(), 
            target.getPath());
   }

   public final native static UnsavedChangesItem create(String id,
         FileIcon icon, String title, String path) /*-{
      return {
         "id"   : id,
         "icon" : icon,
         "title": title,
         "path" : path,
      };
   }-*/;

   @Override
   public final native String getId() /*-{
      return this.id;
   }-*/;

   @Override
   public final native FileIcon getIcon() /*-{
      return this.icon;
   }-*/;

   @Override
   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   @Override
   public final native String getPath() /*-{
      return this.path;
   }-*/;
}
