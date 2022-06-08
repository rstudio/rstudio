/*
 * HelpHyperlink.java
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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class HelpHyperlink extends Hyperlink
{
    public HelpHyperlink(String url, Map<String, String> params, String text, String clazz) 
    {
        super(url, params, text, clazz);
        if (url.startsWith("ide:help:") || url.startsWith("rstudio:help:"))
        {
            String suffix = url.replaceFirst("^.*help:", "");
            String[] splat = suffix.split("::");
            pkg_ = splat[0];
            topic_ = splat[1];
        }
        else 
        {
            topic_ = params.get("topic");
            pkg_ = params.get("package");
        }
        server_ = RStudioGinjector.INSTANCE.getServer();
        helpAvailable_ = false;
    }

    @Override
    public void onClick()
    {
        server_.showHelpTopic(topic_, pkg_, RCompletionType.FUNCTION);
    }

    @Override
    public Widget getPopupContent()
    {
        final VerticalPanel panel = new VerticalPanel();
        
        if (topic_ != null && pkg_ != null)
        {
            HTML label = new HTML("<b>" + topic_ + "</b> {" + pkg_ + "}");
            label.setStyleName(styles_.code());
            panel.add(label);
            
            server_.getHelp(topic_, pkg_, RCompletionType.FUNCTION, new ServerRequestCallback<HelpInfo>()
            {

                @Override
                public void onResponseReceived(HelpInfo response)
                {
                    helpAvailable_ = true;
                    HelpPreview preview = new HelpPreview(response, pkg_, topic_);
                    panel.add(preview);
                }

                @Override
                public void onError(ServerError error)
                {
                    helpAvailable_ = false;
                    Label notFound = new Label("No documentation found");
                    notFound.setStyleName(ConsoleResources.INSTANCE.consoleStyles().promptFullHelp());
                    notFound.addStyleName(styles_.warning());
                    panel.add(notFound);
                }
                
            });
        }

        return panel;
    }

    public String getAnchorClass()
    {
        return styles_.helpHyperlink();
    }
    
    public boolean clickable()
    {
        return helpAvailable_;
    }

    public static boolean handles(String url, Map<String, String> params)
    {
        if (StringUtil.equals(url, "ide:help") || StringUtil.equals(url, "rstudio:help"))
            return params.containsKey("topic") && params.containsKey("package");
        
        return url.matches("^(ide|rstudio):help:(\\w+)::(\\w+)$");
    }

    private String topic_;
    private String pkg_;
    private boolean helpAvailable_;
    private HelpServerOperations server_;
}
