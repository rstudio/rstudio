/*
 * SplitterBeforeResizeEvent.java
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
package com.google.gwt.user.client.ui;

import com.google.gwt.event.shared.GwtEvent;

public class SplitterBeforeResizeEvent extends GwtEvent<SplitterBeforeResizeHandler>
{
   public static final GwtEvent.Type<SplitterBeforeResizeHandler> TYPE =
      new GwtEvent.Type<SplitterBeforeResizeHandler>();

   @Override
   protected void dispatch(SplitterBeforeResizeHandler handler)
   {
      handler.onSplitterBeforeResize(this);
   }

   @Override
   public GwtEvent.Type<SplitterBeforeResizeHandler> getAssociatedType()
   {
      return TYPE;
   }


}

