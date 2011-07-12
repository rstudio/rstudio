/*
 * VcsRefreshEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.GwtEvent;

public class VcsRefreshEvent extends GwtEvent<VcsRefreshHandler>
{
   public static final Type<VcsRefreshHandler> TYPE = new Type<VcsRefreshHandler>();

   public VcsRefreshEvent()
   {
   }

   @Override
   public Type<VcsRefreshHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(VcsRefreshHandler handler)
   {
      handler.onVcsRefresh(this);
   }
}
