/*
 * CommandHyperlink.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CommandHyperlink extends Hyperlink
{
    public CommandHyperlink(String url, String params, String text, String clazz)
    {
        super(url, params, text, clazz);
        command_ = url.replaceFirst("^ide:command:", "");
    }

    @Override
    public String getAnchorClass() 
    {
        return styles_.xtermUnsupportedHyperlink();
    }

    @Override 
    public void onClick(){}

    @Override
    public Widget getPopupContent() 
    {
        VerticalPanel panel = new VerticalPanel();

        Label title = new Label("Unsupported command");
        title.setStyleName(styles_.popupInfo());

        Label commandLabel = new Label(command_);
        commandLabel.setStyleName(styles_.popupCode());

        panel.add(title);
        panel.add(commandLabel);
        return panel;
    }
    
    private String command_;    
}
