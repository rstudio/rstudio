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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class HelpHyperlink extends Hyperlink {

    public HelpHyperlink(String url, String params, String text, String clazz) {
        super(url, params, text, clazz);
        topic_ = params_.get("topic");
        pkg_ = params_.get("package");
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public void onClick() {
        server_.showHelpTopic(topic_, pkg_, RCompletionType.FUNCTION);
    }

    @Override
    public Widget getPopupContent() {
        HTML label = new HTML("Help for " + pkg_ + "::<b>" + topic_ + "</b>()");
        return label;
    }
    
    @Override
    public void setAnchorClass() 
    {
        super.setAnchorClass();
        anchor_.addClassName(AnsiCode.HYPERLINK_STYLE);
    }

    private String topic_;
    private String pkg_;
    private HelpServerOperations server_;
}
