/*
 * HyperlinkPopupPanel.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

public class HyperlinkPopupPanel extends ThemedPopupPanel implements HyperlinkPopupDisplay
{
    public HyperlinkPopupPanel()
    {
        super();
        autoConstrain_ = false;
        setStylePrimaryName(ConsoleResources.INSTANCE.consoleStyles().completionPopup());

        addCloseHandler(new CloseHandler<PopupPanel>()
        {
            @Override
            public void onClose(CloseEvent<PopupPanel> event)
            {
                hide();
            }
        });

        WindowEx.addBlurHandler(new BlurHandler()
        {
            @Override
            public void onBlur(BlurEvent event)
            {
                hide();
            }
        });
    }

    @Override
    public void show(Widget content, PositionCallback callback)
    {
        container_ = new VerticalPanel();
        container_.addStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().hyperlinkPopup());
        
        container_.add(content);
        
        setWidget(container_);

        if (callback != null)
            setPopupPositionAndShow(callback);
        else
            show();
    }

    @Override
    public void setPopupPosition(int left, int top)
    {
        super.setPopupPosition(left, top);
    }

    private VerticalPanel container_;
}
