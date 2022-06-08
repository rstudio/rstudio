package org.rstudio.core.client.hyperlink;

/*
 * HelpHeader.java
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

public class HelpHeader extends Composite
{
    public HelpHeader(String topic, String pkgName)
    {
        HorizontalPanel titlePanel = new HorizontalPanel();
        titlePanel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpTitlePanel());
        Label topicLabel = new Label(topic);
        topicLabel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpTitlePanelTopic());
        titlePanel.add(topicLabel);

        Label pkgLabel = new Label("{" + pkgName + "}");
        pkgLabel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpTitlePanelPackage());
        titlePanel.add(pkgLabel);

        initWidget(titlePanel);
    }
}
