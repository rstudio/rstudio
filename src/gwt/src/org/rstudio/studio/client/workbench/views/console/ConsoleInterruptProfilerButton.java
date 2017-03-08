/*
 * ConsoleInterruptProfilerButton.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.console;

import org.rstudio.core.client.layout.DelayFadeInHelper;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.RprofEvent;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class ConsoleInterruptProfilerButton extends Composite
{
   public static Image CreateProfilerButton()
   {
      ImageResource icon = new ImageResource2x(FileIconResources.INSTANCE.iconProfiler2x());
      Image button = new Image(icon);
      button.addStyleName(ThemeResources.INSTANCE.themeStyles().toolbarButtonLeftImage());
      button.getElement().getStyle().setMarginRight(4,Unit.PX);
      button.setTitle("Profiling Code");
      
      return button;
   }
   
   @Inject
   public ConsoleInterruptProfilerButton(final EventBus events,
                                         Commands commands)
   {
      fadeInHelper_ = new DelayFadeInHelper(this);

      // The SimplePanel wrapper is necessary for the toolbar button's "pushed"
      // effect to work.
      SimplePanel panel = new SimplePanel();
      panel.getElement().getStyle().setPosition(Position.RELATIVE);
      
      ImageResource icon = new ImageResource2x(FileIconResources.INSTANCE.iconProfiler2x());
      Image button = CreateProfilerButton();

      width_ = icon.getWidth() + 6;
      height_ = icon.getHeight();
      panel.setWidget(button);

      initWidget(panel);
      setVisible(false);
      
      events.addHandler(RprofEvent.TYPE, new RprofEvent.Handler()
      {
         @Override
         public void onRprofEvent(RprofEvent event)
         {
            switch (event.getEventType())
            {
               case START:
                  fadeInHelper_.beginShow();
                  break;
               case STOP:
                  fadeInHelper_.hide();
                  break;
               default:
                  break;
            }
         }
      });
   }

   public int getWidth()
   {
      return width_;
   }

   public int getHeight()
   {
      return height_;
   }

   private final DelayFadeInHelper fadeInHelper_;
   private final int width_;
   private final int height_;
}
