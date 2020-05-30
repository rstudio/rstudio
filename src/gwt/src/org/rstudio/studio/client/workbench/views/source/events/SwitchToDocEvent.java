/*
 * SwitchToDocEvent.java
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

import com.google.gwt.event.shared.GwtEvent;

public class SwitchToDocEvent extends GwtEvent<SwitchToDocHandler>
{
   public static final Type<SwitchToDocHandler> TYPE = new Type<SwitchToDocHandler>();

   public SwitchToDocEvent(int selectedIndex)
   {
      selectedIndex_ = selectedIndex;
   }

   public int getSelectedIndex()
   {
      return selectedIndex_;
   }

   @Override
   public Type<SwitchToDocHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(SwitchToDocHandler handler)
   {
      handler.onSwitchToDoc(this);
   }

   private final int selectedIndex_;
}
