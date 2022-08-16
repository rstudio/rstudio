/*
 * WebHyperlink.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;

public class WebHyperlink extends Hyperlink {

    public WebHyperlink(String url, Map<String, String> params, String text, String clazz) 
    {
        super(url, params, text, clazz);
    }

    @Override
    public void onClick() 
    {
        RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(url, new NewWindowOptions());
    }

    @Override
    public void getPopupContent(CommandWithArg<Widget> onReady)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.add(new HyperlinkPopupHeader(url));
        onReady.execute(panel);
    }
    
    public static boolean handles(String url)
    {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
