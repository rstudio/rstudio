/*
 * FileHyperlink.java
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
package org.rstudio.core.client.hyperlink;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.rstudio.studio.client.workbench.views.console.shell.assist.HelpInfoPopupPanelResources;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo.ParsedInfo;

public class HelpPreview extends VerticalPanel
{
    public HelpPreview(HelpInfo info, String pkgName, String topic)
    {
        super();
        pkgName_ = pkgName;
        topic_ = topic;
        
        final HyperlinkResources.HyperlinkStyles styles_ = HyperlinkResources.INSTANCE.hyperlinkStyles();
        setStyleName(RES.styles().helpPopup());
        addStyleName(styles_.helpPreview());

        ParsedInfo parsed = info.parse(pkgName_ + "::" + topic_);
        Label title = new Label(parsed.getTitle());
        title.setStyleName(styles_.helpTitle());

        HTML description = new HTML(parsed.getDescription());
        description.setStyleName(RES.styles().helpBodyText());
        description.addStyleName(styles_.helpDescription());
        
        add(title);
        add(description);
    }

    private static HelpInfoPopupPanelResources RES =
         HelpInfoPopupPanelResources.INSTANCE;
    static {
        RES.styles().ensureInjected();
    }
   
    private String pkgName_;
    private String topic_;
}
