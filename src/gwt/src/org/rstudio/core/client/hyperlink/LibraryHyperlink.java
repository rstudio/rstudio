/*
 * LibraryHyperlink.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.hyperlink;

import java.util.Map;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

public class LibraryHyperlink extends Hyperlink 
{

    public LibraryHyperlink(String url, Map<String, String> params, String text, String clazz)
    {
        super(url, params, text, clazz);
        
        Match match = HYPERLINK_RUN_LIBRARY_PATTERN.match(url, 0);
        package_ = match.getGroup(2);
    }

    @Override
    public void onClick() {
        events_.fireEvent(new SendToConsoleEvent("library(" + package_ + ")", true));
    }

    @Override
    public void getPopupContent(CommandWithArg<Widget> onReady)
    {
        final VerticalPanel panel = new VerticalPanel();

        panel.add(new RunHyperlinkPopupHeader("library(" + package_ + ")"));
        panel.add(new HelpPreview(package_ + "-package", package_, () -> 
        {
            onReady.execute(panel);
        }));
    }
    
    public static boolean handles(String url)
    {
        return HYPERLINK_RUN_LIBRARY_PATTERN.test(url);
    }

    String package_;
    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();
    
    private static final Pattern HYPERLINK_RUN_LIBRARY_PATTERN = Pattern.create("^(x-r-|rstudio:|ide:)run:library[(]([\\w.]+)[)]$", "");
}
