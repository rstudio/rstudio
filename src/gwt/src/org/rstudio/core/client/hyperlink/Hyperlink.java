/*
 * Hyperlink.java
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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.inject.Inject;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.model.VirtualConsoleServerOperations;

public class Hyperlink
{
    public Hyperlink(String url, String params)
    {
        this.url = url;
        this.params = params;
        styles_ = ConsoleResources.INSTANCE.consoleStyles();
        popup_ = new HyperlinkPopupPanel();
    }

    @Inject
    public void initialize(VirtualConsoleServerOperations consoleServer)
    {
        consoleServer_ = consoleServer;
    }

    public String getTitle()
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

    public Element getElement(String text, String clazz)
    {
        if (url.startsWith("rstudio:run") || url.startsWith("ide:run"))
        {
            AnchorElement anchor = Document.get().createAnchorElement();
            anchor.setInnerText(text);
            anchor.addClassName(styles_.xtermCommand());
            if (clazz != null)
                anchor.addClassName(clazz);
            
            String command = getCommand(text, url);
            boolean supported = 
                command.matches("^testthat::snapshot_(accept|review)[(]'\\w+'[)]$") || 
                command.matches("^rlang::last_(error|trace)[(][)]$");

            anchor.addClassName(supported ? styles_.xtermSupportedCommand() : styles_.xtermUnsupportedCommand());
            
            Event.sinkEvents(anchor, Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK);
            Event.setEventListener(anchor, event ->
            {
                if (event.getTypeInt() == Event.ONMOUSEOVER)
                {
                    Rectangle bounds = new Rectangle(anchor.getAbsoluteLeft(), anchor.getAbsoluteBottom(), anchor.getClientWidth(), anchor.getClientHeight());
                    popup_.showCommand(command, supported, new HyperlinkPopupPositioner(bounds, popup_));
                } 
                else if (event.getTypeInt() == Event.ONMOUSEOUT)
                {
                    popup_.hide();
                }
                else if (event.getTypeInt() == Event.ONCLICK) 
                {
                    popup_.hide();
                    events_.fireEvent(new SendToConsoleEvent(command, supported));
                }

            });

            return anchor;
        }
        else 
        {
            AnchorElement anchor = Document.get().createAnchorElement();
            if (clazz != null)
                anchor.addClassName(clazz);
            Event.sinkEvents(anchor, Event.ONCLICK);
        
            Event.setEventListener(anchor, event ->
            {
                consoleServer_.consoleFollowHyperlink(url, text, params, new VoidServerRequestCallback());
            });
            anchor.addClassName(AnsiCode.HYPERLINK_STYLE);
            anchor.setTitle(getTitle());

            anchor.setInnerText(text);
            return anchor;
        }
            
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

    public String url;
    public String params;
    private final ConsoleResources.ConsoleStyles styles_;
    private final HyperlinkPopupPanel popup_;
    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();
    private VirtualConsoleServerOperations consoleServer_;
}