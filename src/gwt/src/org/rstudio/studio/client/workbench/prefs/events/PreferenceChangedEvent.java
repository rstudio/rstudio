/*
 * PreferenceChangedEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.events;

import com.google.gwt.event.shared.GwtEvent;

public class PreferenceChangedEvent extends GwtEvent<PreferenceChangedHandler>
{
   public static final Type<PreferenceChangedHandler> TYPE = new Type<PreferenceChangedHandler>();

   public PreferenceChangedEvent(String prefName)
   {
      prefName_ = prefName;
   }

   public String getName()
   {
      return prefName_;
   }

   @Override
   public Type<PreferenceChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(PreferenceChangedHandler handler)
   {
      handler.onPreferenceChanged(this);
   }

   private final String prefName_;
}
