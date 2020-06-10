/*
 * SourceNavigationEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;

import com.google.gwt.event.shared.GwtEvent;

public class SourceNavigationEvent extends GwtEvent<SourceNavigationHandler>
{
   public static final Type<SourceNavigationHandler> TYPE = new Type<SourceNavigationHandler>();

   public SourceNavigationEvent(SourceNavigation navigation)
   {
      navigation_ = navigation;
   }

   public SourceNavigation getNavigation()
   {
      return navigation_;
   }

   @Override
   public Type<SourceNavigationHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(SourceNavigationHandler handler)
   {
      handler.onSourceNavigation(this);
   }

   private final SourceNavigation navigation_;
}
