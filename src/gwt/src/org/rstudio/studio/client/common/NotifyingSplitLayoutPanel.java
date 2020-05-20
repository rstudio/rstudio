/*
 * NotifyingSplitLayoutPanel.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.BinarySplitLayoutPanel;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.application.events.EventBus;

public class NotifyingSplitLayoutPanel extends SplitLayoutPanel
{
   @Inject
   public NotifyingSplitLayoutPanel(int splitterSize, EventBus events)
   {
      super(splitterSize, true /* keyboardSupport */, BinarySplitLayoutPanel.KEYBOARD_MOVE_SIZE);

      events_ = events;

      addSplitterBeforeResizeHandler(new SplitterBeforeResizeHandler()
      {
         public void onSplitterBeforeResize(SplitterBeforeResizeEvent event)
         {
            events_.fireEvent(new GlassVisibilityEvent(true));
         }
      });

      addSplitterResizedHandler(new SplitterResizedHandler()
      {
         public void onSplitterResized(SplitterResizedEvent event)
         {
            events_.fireEvent(new GlassVisibilityEvent(false));
         }
      });
   }
   
   public void setSplitterEnabled(boolean enabled)
   {
      DomUtils.toggleClass(getElement(), RES.styles().disableSplitter(), !enabled);
   }

   private final EventBus events_;
   
   // Styles, Resources etc. ----
   public interface Styles extends CssResource
   {
      String disableSplitter();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("NotifyingSplitPanel.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
}
