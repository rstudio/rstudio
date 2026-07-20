/*
 * HelpPopoutPanel.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

public class HelpPopoutPanel extends SatelliteFramePanel<RStudioThemedFrame>
{
   @Inject
   public HelpPopoutPanel(Commands commands)
   {
      super(commands);
   }

   @Override
   protected void initToolbar(Toolbar toolbar, Commands commands)
   {
      // the popped-out help window has no toolbar
   }

   public void showHelp(String url)
   {
      showUrl(url, true, null);
   }

   @Override
   protected RStudioThemedFrame createFrame(String url)
   {
      RStudioThemedFrame frame = new RStudioThemedFrame(
         constants_.helpText(),
         url,
         HelpPane.getEditorStyles(),
         null,
         false,
         true);

      frame.addStyleName("ace_editor_theme");
      return frame;
   }

   private static final HelpConstants constants_ = GWT.create(HelpConstants.class);
}
