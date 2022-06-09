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

import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.Rectangle;

public abstract class Hyperlink
{
    public Hyperlink(String url, Map<String, String> params, String text, String clazz)
    {
        this.url = url;
        this.text = text;
        this.clazz = clazz;
        this.params = params;

        anchor_ = Document.get().createAnchorElement();
        styles_ = RES.hyperlinkStyles();
        popup_ = new HyperlinkPopupPanel();
    }

    public Element getElement()
    {
        anchor_.setInnerText(text);
        anchor_.setClassName(getAnchorClass());
        if (clazz != null)
            anchor_.addClassName(clazz);
        
        Event.sinkEvents(anchor_, Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK);
        Event.setEventListener(anchor_, event ->
        {
            if (event.getTypeInt() == Event.ONMOUSEOVER)
            {
                Rectangle bounds = new Rectangle(anchor_.getAbsoluteLeft(), anchor_.getAbsoluteTop(), anchor_.getClientWidth(), anchor_.getClientHeight());
                popup_.show(getPopupContent(), new HyperlinkPopupPositioner(bounds, popup_));
            } 
            else if (event.getTypeInt() == Event.ONMOUSEOUT)
            {
                popup_.hide();
            }
            else if (event.getTypeInt() == Event.ONCLICK && clickable()) 
            {
                popup_.hide();
                onClick();
            }
        });

        return anchor_;
    }

    public String getAnchorClass()
    {
        return styles_.xtermHyperlink();
    }

    public boolean clickable()
    {
        return true;
    }
    public abstract void onClick();
    public abstract Widget getPopupContent();
    
    public static Hyperlink create(String url, String paramsTxt, String text, String clazz)
    {
        // [params] of the form key1=value1:key2=value2
        Map<String, String> params = new TreeMap<>();
        if (paramsTxt.length() > 0)
        {
            for (String param: paramsTxt.split(":"))
            {
                String[] bits = param.split("=");
                String key = bits[0].trim();
                String value = bits[1].trim();
                params.put(key, value);
            }
        }
        if (FileHyperlink.handles(url))
        {
            return new FileHyperlink(url, params, text, clazz);
        }
        else if (WebHyperlink.handles(url))
        {
            return new WebHyperlink(url, params, text, clazz);
        }
        else if (HelpHyperlink.handles(url, params))
        {
            return new HelpHyperlink(url, params, text, clazz);
        }
        else if (VignetteHyperlink.handles(url, params))
        {
            return new VignetteHyperlink(url, params, text, clazz);
        }
        else if (RunHyperlink.handles(url))
        {
            return new RunHyperlink(url, params, text, clazz);
        }
        else 
        {
            return new UnsupportedHyperlink(url, params, text, clazz);
        }    
    }

    public String url;
    public String text;
    public String clazz;
    public Map<String, String> params;
    
    protected AnchorElement anchor_;

    protected final HyperlinkResources.HyperlinkStyles styles_;
    private final HyperlinkPopupPanel popup_;

    private static HyperlinkResources RES = HyperlinkResources.INSTANCE;
}
