/*
 * ShowErrorMessageEvent.java
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
import org.rstudio.studio.client.workbench.model.ErrorMessage;

public class ShowErrorMessageEvent extends GwtEvent<ShowErrorMessageHandler>
{
   public static final GwtEvent.Type<ShowErrorMessageHandler> TYPE =
      new GwtEvent.Type<ShowErrorMessageHandler>();
   
   public ShowErrorMessageEvent(ErrorMessage message)
   {
      errorMessage_ = message;
   }
   
   public ErrorMessage getErrorMessage()
   {
      return errorMessage_;
   }
   
   @Override
   protected void dispatch(ShowErrorMessageHandler handler)
   {
      handler.onShowErrorMessage(this);
   }

   @Override
   public GwtEvent.Type<ShowErrorMessageHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private ErrorMessage errorMessage_;
}
