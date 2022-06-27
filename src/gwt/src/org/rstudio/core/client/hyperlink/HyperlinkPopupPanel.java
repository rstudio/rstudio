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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

public class HyperlinkPopupPanel extends ThemedPopupPanel implements HyperlinkPopupDisplay
{
    public HyperlinkPopupPanel(HelpPageShower hyperlink)
    {
        super();
        hyperlink_ = hyperlink;
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
        
        handler_ = new NativePreviewHandler()
        {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent previewEvent)
            {
                // any click outside the container or help popup should dismiss
                if (previewEvent.getTypeInt() == Event.ONMOUSEDOWN)
                {
                    Element targetEl = previewEvent.getNativeEvent().getEventTarget().cast();
                    if (!container_.getElement().isOrHasChild(targetEl) &&
                        (container_ == null) || 
                        (container_.getElement() == null) || 
                        !container_.getElement().isOrHasChild(targetEl))
                    {
                        hide();
                    }
                }
                
                if (previewEvent.getTypeInt() == Event.ONKEYDOWN)
                {
                    NativeEvent event = previewEvent.getNativeEvent();
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyCodes.KEY_ESCAPE)
                    {
                        event.stopPropagation();
                        hide();
                    }
                    else if (keyCode == KeyCodes.KEY_F1)
                    {
                        hyperlink_.showHelp();
                    }
                }
            }
        };
    }

    public void setContent(Widget content)
    {
        if (current_ != null)
        {
            current_.hide();
            current_ = null;
        }
        container_ = new VerticalPanel();
        container_.addStyleName(HyperlinkResources.INSTANCE.hyperlinkStyles().hyperlinkPopup());
        setWidget(container_);
        container_.add(content);

        registerNativeHandler(handler_);
        current_ = this;
    }

    @Override
    public void setPopupPosition(int left, int top)
    {
        super.setPopupPosition(left, top);
    }

    @Override
    public void hide()
    {
        unregisterNativeHandler();
        super.hide();
    }

    private void registerNativeHandler(NativePreviewHandler handler)
    {
        if (handlerRegistration_ != null)
            handlerRegistration_.removeHandler();
        handlerRegistration_ = Event.addNativePreviewHandler(handler);
    }

    private void unregisterNativeHandler()
    {
        if (handlerRegistration_ != null)
            handlerRegistration_.removeHandler();
    }

    private VerticalPanel container_;
    private final NativePreviewHandler handler_;
    private HandlerRegistration handlerRegistration_;
    private HelpPageShower hyperlink_;
   
    private static HyperlinkPopupPanel current_;
}
