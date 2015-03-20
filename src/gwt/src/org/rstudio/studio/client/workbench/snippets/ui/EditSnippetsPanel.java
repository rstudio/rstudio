/*
 * EditSnippetsPanel.java
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

package org.rstudio.studio.client.workbench.snippets.ui;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class EditSnippetsPanel extends FullscreenPopupPanel
{
   public EditSnippetsPanel()
   {
      super(createTitlePanel(),
            createMainPanel(),
            100,
            true);
   }
   
   static Widget createMainPanel()
   {
      return new SimplePanel();
   }
   
   static HorizontalPanel createTitlePanel()
   {
      HorizontalPanel titlePanel = new HorizontalPanel();
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      Label titleLabel = new Label("Edit Snippets");
      titleLabel.addStyleName(styles.fullscreenCaptionLabel());
      titlePanel.add(titleLabel);
      return titlePanel;
   }
}
