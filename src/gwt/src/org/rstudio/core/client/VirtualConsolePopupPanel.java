/*
 * VirtualConsolePopupPanel.java
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
package org.rstudio.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

public class VirtualConsolePopupPanel extends ThemedPopupPanel implements VirtualConsolePopupDisplay
{
    public VirtualConsolePopupPanel()
    {
        super();
        autoConstrain_ = false;
        styles_ = ConsoleResources.INSTANCE.consoleStyles();

        setStylePrimaryName(styles_.completionPopup());

        addCloseHandler(new CloseHandler<PopupPanel>(){

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
    public void showCommand(String command, boolean supported, PositionCallback callback)
    {
        container_ = new VerticalPanel();
        container_.addStyleName(styles_.popupPanel());
        Label code = new Label(command);
        code.setStyleName(styles_.popupCode());
        
        HTML altClick = new HTML("<b>alt-click</b> to copy the code to the console without executing it.");
        altClick.setStyleName(styles_.popupInfo());
        
        container_.add(code);
        if (supported)
        {
            HTML click = new HTML("<b>click</b> to run the code in the console.");
            click.setStyleName(styles_.popupInfo());
            container_.add(click);
        }
        container_.add(altClick);
    
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

    private final ConsoleResources.ConsoleStyles styles_;
    private VerticalPanel container_;
    private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
