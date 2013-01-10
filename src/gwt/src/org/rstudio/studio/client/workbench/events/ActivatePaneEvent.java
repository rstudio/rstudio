/*
 * ActivatePaneEvent.java
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
package org.rstudio.studio.client.workbench.events;


import com.google.gwt.event.shared.GwtEvent;


public class ActivatePaneEvent extends GwtEvent<ActivatePaneHandler>
{
   public static final GwtEvent.Type<ActivatePaneHandler> TYPE =
      new GwtEvent.Type<ActivatePaneHandler>();
   
   public ActivatePaneEvent(String pane)
   {
      pane_ = pane;
   }
   
   public String getPane()
   {
      return pane_;
   }
   
   @Override
   protected void dispatch(ActivatePaneHandler handler)
   {
      handler.onActivatePane(this);
   }

   @Override
   public GwtEvent.Type<ActivatePaneHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String pane_;
}