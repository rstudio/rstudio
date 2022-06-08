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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class WebHyperlink extends Hyperlink {

    public WebHyperlink(String url, Map<String, String> params, String text, String clazz) 
    {
        super(url, params, text, clazz);
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public void onClick() 
    {
        String code = "utils::browseURL('" + url + "')";
        server_.executeRCode(code, new ServerRequestCallback<String>(){
            @Override
            public void onResponseReceived(String response) {}

            @Override
            public void onError(ServerError error) {}
        });
    }

    @Override
    public Widget getPopupContent() 
    {
        VerticalPanel panel = new VerticalPanel();

        Label urlLabel = new Label(url);
        urlLabel.setStyleName(styles_.code());
        panel.add(urlLabel);

        /*
            // TODO: maybe we can show a screenshot ?
            //       perhaps using {webshot} ?
            Frame frame = new Frame(url);
            panel.add(frame);
        */

        return panel;
    }
    
    public static boolean handles(String url)
    {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private SourceServerOperations server_;
}
