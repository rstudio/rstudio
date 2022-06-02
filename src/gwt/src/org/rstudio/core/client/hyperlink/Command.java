/*
 * Command.java
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
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

public class Command extends Hyperlink
{
    public Command(String url, String params, String text, String clazz)
    {
        super(url, params, text, clazz);
        
        command_ = getCommand(text, url);
        supported_ = 
            command_.matches("^testthat::snapshot_(accept|review)[(]'\\w+'[)]$") || 
            command_.matches("^rlang::last_(error|trace)[(][)]$");
    }

    @Override
    public String getAnchorClass() 
    {
        return styles_.xtermCommand();
    }

    @Override 
    public void onClick()
    {
        events_.fireEvent(new SendToConsoleEvent(command_, supported_));
    }

    @Override
    public Widget getPopupContent() {
        Label code = new Label(command_);
        code.setStyleName(styles_.popupCode());
        return code;
    }

    private String getCommand(String text, String url)
    {
        String command = text;
        if (url.startsWith("rstudio:run:"))
        {
            command = url.replaceFirst("rstudio:run:", "");
        }
        else if (url.startsWith("ide:run:"))
        {
            command = url.replaceFirst("ide:run:", "");
        }
        return command;
    }

    private String command_;
    private boolean supported_;

    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();
    
}
