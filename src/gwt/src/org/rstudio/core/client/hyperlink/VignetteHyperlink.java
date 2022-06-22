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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Server;

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
    public void getPopupContent(CommandWithArg<Widget> onReady)
    {
        final VerticalPanel panel = new VerticalPanel();
        HyperlinkPopupHeader header = new HyperlinkPopupHeader(topic_, "{" + pkg_ + "}");
        panel.add(header);

        server_.isPackageInstalled(pkg_, null, new SimpleRequestCallback<Boolean>()
        {
            @Override
            public void onResponseReceived(Boolean installed)
            {
                if (!installed)
                {
                    panel.add(new WarningLabel(constants_.noPackage()));
                    onReady.execute(panel);
                }
                else 
                {
                    server_.getVignetteTitle(topic_, pkg_, new SimpleRequestCallback<String>()
                    {
                        @Override
                        public void onResponseReceived(String response)
                        {
                            if (response.length() > 0) 
                            {
                                VerticalPanel previewPanel = new VerticalPanel();
                                previewPanel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreview());
            
                                Label title = new Label(response);
                                title.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreviewTitle());
                                previewPanel.add(title);

                                server_.getVignetteDescription(topic_, pkg_, new SimpleRequestCallback<String>()
                                {
                                    @Override
                                    public void onResponseReceived(String description)
                                    {
                                        if (description.length() > 0)
                                        {
                                            Label descriptionLabel = new Label(description);
                                            descriptionLabel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreviewDescription());
                                            previewPanel.add(descriptionLabel);
                                        }

                                        panel.add(previewPanel);
                                        onReady.execute(panel);
                                    }
                                });
                            }
                            else 
                            {
                                panel.add(new WarningLabel(constants_.noVignette()));
                            }
                            onReady.execute(panel);
                        }
                    });
                }

            }
        });

        
    }

    public static boolean handles(String url, Map<String, String> params)
    {
        if (StringUtil.equals(url, "ide:vignette") || StringUtil.equals(url, "rstudio:vignette"))
            return params.containsKey("topic") && params.containsKey("package");
        
        return url.matches("^(ide|rstudio):vignette:(\\w+)::(\\w+)$");
    }
    
    private String topic_;
    private String pkg_;
    private Server server_;

    private static final HyperlinkConstants constants_ = GWT.create(HyperlinkConstants.class);
}
