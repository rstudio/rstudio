/*
 * DocTabsChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.resources.client.ImageResource;

public class DocTabsChangedEvent extends GwtEvent<DocTabsChangedHandler>
{
   public static final Type<DocTabsChangedHandler> TYPE = new Type<DocTabsChangedHandler>();

   public DocTabsChangedEvent(String[] ids,
                              ImageResource[] icons,
                              String[] names,
                              String[] paths)
   {
      ids_ = ids;
      this.icons = icons;
      this.names = names;
      this.paths = paths;
   }

   public String[] getIds()
   {
      return ids_;
   }

   public ImageResource[] getIcons()
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

   private final String[] ids_;
   private final ImageResource[] icons;
   private final String[] names;
   private final String[] paths;
}
