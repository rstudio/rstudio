/*
 * ShowHelpEvent.java
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
package org.rstudio.studio.client.workbench.views.help.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class ShowHelpEvent extends CrossWindowEvent<ShowHelpHandler>
{
   public static final GwtEvent.Type<ShowHelpHandler> TYPE =
      new GwtEvent.Type<ShowHelpHandler>();
   
   public ShowHelpEvent()
   {
   }

   public ShowHelpEvent(String topicUrl)
   {
      topicUrl_ = topicUrl;
   }
   
   public String getTopicUrl()
   {
      return topicUrl_;
   }
   
   @Override
   public int focusMode()
   {
      return CrossWindowEvent.MODE_AUXILIARY;
   }
   
   @Override
   protected void dispatch(ShowHelpHandler handler)
   {
      handler.onShowHelp(this);
   }

   @Override
   public GwtEvent.Type<ShowHelpHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String topicUrl_;
}