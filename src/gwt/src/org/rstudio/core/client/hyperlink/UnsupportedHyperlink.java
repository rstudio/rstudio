/*
 * UnsupportedHyperlink.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.CommandWithArg;

public class UnsupportedHyperlink extends Hyperlink
{

    public UnsupportedHyperlink(String url, Map<String, String> params, String text, String clazz)
    {
        super(url, params, text, clazz);
    }

    @Override
    public String getAnchorClass() 
    {
        return styles_.hyperlinkUnsupported();
    }

    @Override
    public void onClick() {}

    @Override
    public void getPopupContent(CommandWithArg<Widget> onReady)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.add(new HyperlinkPopupHeader(url));

        if (!params.isEmpty())
        {
            for (Map.Entry<String, String> param: params.entrySet())
            {
                Label paramLabel = new Label();
                paramLabel.setStyleName(styles_.warning());
                panel.add(new WarningLabel(param.getKey() + ": " + param.getValue()));
            }

        }
        
        onReady.execute(panel);
    }
}
