/*
 * RlangCommandHyperlink.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

public class RlangCommandHyperlink extends Hyperlink {

    public RlangCommandHyperlink(String url, String params, String text, String clazz) {
        super(url, params, text, clazz);
        fn_ = url.replaceFirst("ide:command:", "");
        
        if (StringUtil.equals(fn_, "rlang::last_error") || StringUtil.equals(fn_, "rlang::last_trace"))
        {
            code_ = fn_ + "()";
        }
    }

    @Override
    public void onClick() {
        if (code_ != null)
        {
            events_.fireEvent(new SendToConsoleEvent(code_, true));
        }
    }

    @Override
    public Widget getPopupContent() {
        if (code_ == null)
            return null;
        
        VerticalPanel panel = new VerticalPanel();

        Label title = new Label(
            StringUtil.equals(fn_, "rlang::last_error") ? 
            "Last error" : 
            "Last trace"
        );
        title.setStyleName(styles_.popupInfo());

        Label codeLabel = new Label(code_);
        codeLabel.setStyleName(styles_.popupCode());

        panel.add(title);
        panel.add(codeLabel);

        return panel;
    }

    @Override
    public String getAnchorClass() 
    {
        return code_ == null ? styles_.xtermUnsupportedHyperlink() : styles_.xtermCommand();
    }
    
    private String fn_;
    private String code_;

    private static final EventBus events_ = RStudioGinjector.INSTANCE.getEventBus();  

}
