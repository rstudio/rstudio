/*
 * VignetteHyperlink.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class VignetteHyperlink extends Hyperlink 
{
    public VignetteHyperlink(String url, Map<String, String> params, String text, String clazz) 
    {
        super(url, params, text, clazz);
        if (url.contains(":vignette:"))
        {
            String[] splat = url.replaceFirst("^.*vignette:", "").split("::");
            pkg_ = splat[0];
            topic_ = splat[1];
        }
        else 
        {
            topic_ = params.get("topic");
            pkg_ = params.get("package");
        }
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public void onClick() 
    {
        server_.showVignette(topic_, pkg_);
    }

    @Override
    public Widget getPopupContent() 
    {
        final VerticalPanel panel = new VerticalPanel();
        panel.add(new HyperlinkPopupHeader("Vignette: " + topic_, "{" + pkg_ + "}"));

        server_.getVignetteTitle(topic_, pkg_, new SimpleRequestCallback<String>()
        {
            @Override
            public void onResponseReceived(String response)
            {
                if (response.length() > 0) 
                {
                    VerticalPanel helpPanel = new VerticalPanel();

                    helpPanel.addStyleName(styles_.helpPreview());

                    Label title = new Label(response);
                    title.setStyleName(styles_.helpPreviewDescription());
                    helpPanel.add(title);

                    panel.add(helpPanel);
                }
                else 
                {
                    Label notFound = new Label("No vignette found");
                    notFound.setStyleName(styles_.warning());
                    notFound.addStyleName(ConsoleResources.INSTANCE.consoleStyles().promptFullHelp());
                    panel.add(notFound);
                }
            }
        });
        
        return panel;
    }

    public static boolean handles(String url, Map<String, String> params)
    {
        if (StringUtil.equals(url, "ide:vignette") || StringUtil.equals(url, "rstudio:vignette"))
            return params.containsKey("topic") && params.containsKey("package");
        
        return url.matches("^(ide|rstudio):vignette:(\\w+)::(\\w+)$");
    }
    
    private String topic_;
    private String pkg_;
    private HelpServerOperations server_;
}
