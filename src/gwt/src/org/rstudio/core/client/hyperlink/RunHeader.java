package org.rstudio.core.client.hyperlink;

/*
 * RunHeader.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class RunHeader extends Composite
{

   public RunHeader(String fn) {
      this(fn, "(click to run)");
   }

   public RunHeader(String fn, String hint)
   {
      HyperlinkResources.HyperlinkStyles styles = HyperlinkResources.INSTANCE.hyperlinkStyles();

      HorizontalPanel titlePanel = new HorizontalPanel();
      titlePanel.setStyleName(styles.helpTitlePanel());
      Label fnLabel = new Label(fn);
      fnLabel.setStyleName(styles.helpTitlePanelTopic());
      titlePanel.add(fnLabel);

      Label hintLabel = new Label(hint);
      hintLabel.setStyleName(styles.helpTitlePanelPackage());
      titlePanel.add(hintLabel);

      initWidget(titlePanel);
   }
}
