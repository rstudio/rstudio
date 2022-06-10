/*
 * HelpPreview.java
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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo.ParsedInfo;

public class HelpPreview extends Composite
{
    public HelpPreview(String topic, String pkgName, Command onReady)
    {
        super();
        
        topic_ = topic;
        pkgName_ = pkgName;
        server_ = RStudioGinjector.INSTANCE.getServer();
        panel_ = new VerticalPanel();
        
        initWidget(panel_);

        server_.isPackageInstalled(pkgName_, null, new SimpleRequestCallback<Boolean>()
        {
            @Override
            public void onResponseReceived(Boolean installed)
            {
                if (installed)
                {
                    server_.getHelp(topic_, pkgName_, RCompletionType.FUNCTION, new SimpleRequestCallback<HelpInfo>()
                    {
                        @Override
                        public void onResponseReceived(HelpInfo response)
                        {
                            if (response != null)
                            {
                                VerticalPanel previewPanel = new VerticalPanel();
                                previewPanel.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreview());
            
                                ParsedInfo parsed = response.parse(pkgName_ + "::" + topic_);
                                Label title = new Label(parsed.getTitle());
                                title.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreviewTitle());
            
                                HTML description = new HTML(parsed.getDescription());
                                description.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().helpPreviewDescription());
                                
                                previewPanel.add(title);
                                previewPanel.add(description);
                                panel_.add(previewPanel);
                            }
                            else 
                            {
                                Label notFound = new Label("No documentation found");
                                notFound.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().warning());
                                panel_.add(notFound);
                            }
            
                            onReady.execute();
                        }            
                    });
                } 
                else 
                {
                    Label notInstalled = new Label("package " + pkgName + " is not installed");
                    notInstalled.setStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().warning());
                    panel_.add(notInstalled);
                    onReady.execute();
                }
            }
        });
        
    }
   
    private VerticalPanel panel_;
    
    private String topic_;
    private String pkgName_;
    private Server server_;
}
