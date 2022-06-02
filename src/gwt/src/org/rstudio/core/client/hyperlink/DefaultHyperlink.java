/*
 * DefaultHyperlink.java
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
import com.google.inject.Inject;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.model.VirtualConsoleServerOperations;

public class DefaultHyperlink extends Hyperlink
{

    public DefaultHyperlink(String url, String params, String text, String clazz) {
        super(url, params, text, clazz);
    }

    @Inject
    public void initialize(VirtualConsoleServerOperations consoleServer)
    {
        consoleServer_ = consoleServer;
    }

    private String getTitle()
    {
        if (url.startsWith("rstudio:viewer:")) 
        {
            return "open in viewer: " + url.replace("rstudio:viewer:", "");
        }
        else if (StringUtil.equals(url, "rstudio:help")) 
        {
            return "help(" + params.replace(":", ", ") + ")";
        }
        else if (StringUtil.equals(url, "rstudio:vignette")) 
        {
            return "vignette(" + params.replace(":", ", ") + ")";
        }
        else
        {
            return url;
        }
    }

    @Override
    public void setAnchorClass() 
    {
        super.setAnchorClass();
        anchor_.addClassName(AnsiCode.HYPERLINK_STYLE);
    }

    @Override
    public void onClick() {
        consoleServer_.consoleFollowHyperlink(url, text, params, new VoidServerRequestCallback());
    }

    @Override
    public Widget getPopupContent() {
        return new Label(getTitle());
    }
    
    private VirtualConsoleServerOperations consoleServer_;

}
