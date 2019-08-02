/*
 * DocTabsChangedEvent.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.common.filetypes.FileIcon;

public class DocTabsChangedEvent extends GwtEvent<DocTabsChangedHandler>
{
   public static final Type<DocTabsChangedHandler> TYPE = new Type<DocTabsChangedHandler>();

   public DocTabsChangedEvent(String activeId,
                              String[] ids,
                              FileIcon[] icons,
                              String[] names,
                              String[] paths)
   {
      activeId_ = activeId;
      ids_ = ids;
      this.icons = icons;
      this.names = names;
      this.paths = paths;
   }

   public String getActiveId()
   {
      return activeId_;
   }
   
   public String[] getIds()
   {
      return ids_;
   }

   public FileIcon[] getIcons()
   {
      return icons;
   }

   public String[] getNames()
   {
      return names;
   }

   public String[] getPaths()
   {
      return paths;
   }

   @Override
   public Type<DocTabsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DocTabsChangedHandler handler)
   {
      handler.onDocTabsChanged(this);
   }

   private final String activeId_;
   private final String[] ids_;
   private final FileIcon[] icons;
   private final String[] names;
   private final String[] paths;
}
