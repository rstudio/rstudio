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

import java.util.Map;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;

public class RunHyperlink extends Hyperlink
{
    public RunHyperlink(String url, Map<String, String> params, String text, String clazz)
    {
        super(url, params, text, clazz);
        code_ = url.replaceFirst("^(ide|rstudio):run:", "");
        Match match = HYPERLINK_PATTERN.match(code_, 0);
        package_ = match.getGroup(2);
        fun_ = match.getGroup(3);
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public String getAnchorClass() 
    {
        return styles_.xtermCommand();
    }

    @Override 
    public void onClick()
    {
        server_.isPackageHyperlinkSafe(package_, new SimpleRequestCallback<Boolean>(){

            @Override
            public void onResponseReceived(Boolean response)
            {
                events_.fireEvent(new SendToConsoleEvent(code_, response));
            }
            
        });
    }

    @Override
    public Widget getPopupContent() 
    {
        final VerticalPanel panel = new VerticalPanel();

        Label commandLabel = new Label(code_);
        commandLabel.setStyleName(styles_.code());
        panel.add(commandLabel);
        
        server_.getHelp(fun_, package_, RCompletionType.FUNCTION, new SimpleRequestCallback<HelpInfo>()
        {
            @Override
            public void onResponseReceived(HelpInfo response)
            {
                HelpPreview preview = new HelpPreview(response, package_, fun_);
                
                HTML more = new HTML("Click to run the code in the console.");
                more.setStyleName(ConsoleResources.INSTANCE.consoleStyles().promptFullHelp());
                preview.add(more);

                panel.add(preview);
            }
            
        });
    
    return panel;
    }

    public static boolean handles(String url)
    {
        return HYPERLINK_PATTERN.test(url);
    }
    
    private String code_;    
    private String package_;
    private String fun_;
    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();
    private Server server_;

    // allow code of the form pkg::fn(<args>) where args does not have ;()
    private static final Pattern HYPERLINK_PATTERN = Pattern.create("^(rstudio|ide):run:(\\w+)::(\\w+)[(][^();]*[)]$");
}
