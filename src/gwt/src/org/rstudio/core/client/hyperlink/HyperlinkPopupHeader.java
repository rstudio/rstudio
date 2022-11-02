/*
 * HyperlinkPopupHeader.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client.hyperlink;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class HyperlinkPopupHeader extends Composite
{
    public HyperlinkPopupHeader(String left)
    {
        this(left, "");
    }

    public HyperlinkPopupHeader(String left, String right)
    {
        HyperlinkResources.HyperlinkStyles styles = HyperlinkResources.INSTANCE.hyperlinkStyles();

        HorizontalPanel titlePanel = new HorizontalPanel();
        titlePanel.setStyleName(styles.hyperlinkPopupHeader());

        topicLabel = new Label(left);
        topicLabel.setStyleName(styles.hyperlinkPopupHeaderLeft());
        titlePanel.add(topicLabel);

        pkgLabel = new Label(right);
        pkgLabel.setStyleName(styles.hyperlinkPopupHeaderRight());
            
        if (right.length() > 0)
        {
            titlePanel.add(pkgLabel);
        }
        
        initWidget(titlePanel);
    }

    public Label topicLabel;
    public Label pkgLabel;
}
