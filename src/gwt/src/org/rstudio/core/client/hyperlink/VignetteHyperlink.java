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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class VignetteHyperlink extends Hyperlink 
{
    public VignetteHyperlink(String url, String params, String text, String clazz) 
    {
        super(url, params, text, clazz);
        topic_ = params_.get("topic");
        pkg_ = params_.get("package");
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

        HTML label = new HTML("<b>" + topic_ + "</b> {" + pkg_ + "} <hr/>");
        label.setStyleName(styles_.popupCode());
        panel.add(label);

        server_.getVignetteTitle(topic_, pkg_, new ServerRequestCallback<String>() {

            @Override
            public void onResponseReceived(String response) {
                VerticalPanel helpPanel = new VerticalPanel();
                helpPanel.setStyleName(styles_.popupHelpPanel());
                Label title = new Label(response);
                title.setStyleName(styles_.popupHelpTitle());

                helpPanel.add(title);
                
                panel.add(helpPanel);
            }

            @Override
            public void onError(ServerError error) {
                Label notFound = new Label("No vignette found for `"+topic_+"` in package {" + pkg_ + "}");
                notFound.setStyleName(styles_.popupWarning());
                panel.add(notFound);
            }
               
        });
        
        return panel;
    }
    
    private String topic_;
    private String pkg_;
    private HelpServerOperations server_;
}
