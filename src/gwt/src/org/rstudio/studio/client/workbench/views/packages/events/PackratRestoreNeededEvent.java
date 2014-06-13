/*
 * PackratRestoreNeededEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.packages.events;

import org.rstudio.studio.client.packrat.model.PackratPackageAction;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PackratRestoreNeededEvent extends GwtEvent<PackratRestoreNeededEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onPackratRestoreNeeded(PackratRestoreNeededEvent event);
   }

   public PackratRestoreNeededEvent(JsArray<PackratPackageAction> actions)
   {
      actions_ = actions;
   }

   public JsArray<PackratPackageAction> getActions()
   {
      return actions_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackratRestoreNeeded(this);
   }

   private final JsArray<PackratPackageAction> actions_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}