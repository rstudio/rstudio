/*
 * CommandHighlighter.java
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
package org.rstudio.studio.client.workbench.commands;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.HighlightCommandEvent;
import org.rstudio.studio.client.application.events.EventBus;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CommandHighlighter
      implements HighlightCommandEvent.Handler
{
   @Inject
   public CommandHighlighter(EventBus events)
   {
      events.addHandler(HighlightCommandEvent.TYPE, this);
   }
   

   @Override
   public void onHighlightCommand(HighlightCommandEvent event)
   {
      String commandId = event.getData().getId();
      highlight(commandId.toLowerCase());
   }
   
   
   private void highlight(String elementClass)
   {
      for (Element el : highlightedEls_)
      {
         if (el != null)
         {
            el.removeClassName(RES.styles().pulse());
         }
      }
      
      highlightedEls_ = DomUtils.getElementsByClassName(elementClass);
      
      for (Element el : highlightedEls_)
      {
         el.addClassName(RES.styles().pulse());
      }
   }
   

   public interface Styles extends CssResource
   {
      String pulse();
   }

   public interface Resources extends ClientBundle
   {
      @Source("CommandHighlighter.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

 
   private Element[] highlightedEls_ = new Element[] {};
   
   // Keyframes are not supported by GWT CSS, so we have to hack around it
   private static final String CSS_COMMAND_PULSE_KEYFRAMES = 
         "@keyframes pulse {" +
         "0%   { box-shadow: 0 0 2px 1px rgb(255, 255, 255, 1); }" +
         "100% { box-shadow: 0 0 2px 1px rgb(255, 255, 255, 0); }" +
         "}";
         
}
