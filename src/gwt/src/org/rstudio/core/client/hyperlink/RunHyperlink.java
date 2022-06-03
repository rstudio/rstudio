/*
 * RunHyperlink.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

public class RunHyperlink extends Hyperlink
{
    public RunHyperlink(String url, String params, String text, String clazz)
    {
        super(url, params, text, clazz);
        code_ = url.replaceFirst("^(ide|rstudio):run:", "");
        Match match = HYPERLINK_PATTERN.match(code_, 0);
        if (match == null)
        {
            package_ = null;
        }
        else 
        {
            package_ = match.getGroup(1);
        }
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public String getAnchorClass() 
    {
        return (package_ == null) ? styles_.xtermUnsupportedHyperlink() : styles_.xtermCommand();
    }

    @Override 
    public void onClick(){
        if (package_ != null)
        {
            server_.getPackageHyperlinkRisk(package_, new ServerRequestCallback<String>(){

                @Override
                public void onResponseReceived(String response) {
                    if (StringUtil.equals(response, "loaded") || StringUtil.equals(response, "allowed"))
                    {
                        // package is loaded or in the allow list (e.g. testthat)
                        events_.fireEvent(new SendToConsoleEvent(code_, true));
                    }
                    else if (StringUtil.equals(response, "installed")) 
                    {
                        // package is installed but not loaded
                        events_.fireEvent(new SendToConsoleEvent(code_, false));
                    }
                    else
                    {
                        // package is not installed, make the code a comment
                        // maybe this should offer to install it ?
                        events_.fireEvent(new SendToConsoleEvent("# " + code_, false));
                    }
                }
    
                @Override
                public void onError(ServerError error) {}
                
            });
        }
    }

    @Override
    public Widget getPopupContent() 
    {
        VerticalPanel panel = new VerticalPanel();

        Label commandLabel = new Label(code_);
        commandLabel.setStyleName( 
            (package_ == null) ? styles_.popupArbitraryCode() : styles_.popupCode()
        );
        panel.add(commandLabel);
        
        if (package_ == null)
        {
            HTML hint = new HTML("Code must be of the form <b>pkg::fun(...)</b>.");
            hint.setStyleName(styles_.popupWarning());
            panel.add(hint);
        } 
        else
        {
            Label title = new Label("Run code in the console.");
            title.setStyleName(styles_.popupInfo());
            panel.add(title);
        }

        return panel;
    }
    
    private String code_;    
    private String package_;
    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();
    private PackagesServerOperations server_;

    // allow code of the form pkg::fn(<args>) where args does not have ;()
    private static final Pattern HYPERLINK_PATTERN = Pattern.create("^(\\w+)::(\\w+)[(][^();]*[)]$");
}
