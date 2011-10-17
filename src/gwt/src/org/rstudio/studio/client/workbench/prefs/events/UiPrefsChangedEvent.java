/*
 * UiPrefsChangedEvent.java
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
package org.rstudio.studio.client.workbench.prefs.events;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.event.shared.GwtEvent;

public class UiPrefsChangedEvent extends GwtEvent<UiPrefsChangedHandler>
{
   public static final Type<UiPrefsChangedHandler> TYPE = new Type<UiPrefsChangedHandler>();

   public UiPrefsChangedEvent(JsObject uiPrefs)
   {
      uiPrefs_ = uiPrefs;
   }

   public JsObject getUIPrefs()
   {
      return uiPrefs_;
   }

   @Override
   public Type<UiPrefsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(UiPrefsChangedHandler handler)
   {
      handler.onUiPrefsChanged(this);
   }

   private final JsObject uiPrefs_;
}
