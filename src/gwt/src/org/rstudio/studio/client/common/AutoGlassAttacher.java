/*
 * AutoGlassAttacher.java
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

import com.google.gwt.user.client.ui.LayoutPanel;
import org.rstudio.core.client.widget.GlassAttacher;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

public class AutoGlassAttacher extends GlassAttacher
{
   public AutoGlassAttacher(LayoutPanel panel)
   {
      super(panel);

      EventBus eventBus = RStudioGinjector.INSTANCE.getEventBus();
      eventBus.addHandler(GlassVisibilityEvent.TYPE, glassVisibilityEvent ->
      {
         setGlass(glassVisibilityEvent.isShow());
      });
      setGlass(false);
   }
}
