/*
 * HyperlinkResources.java
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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface HyperlinkResources extends ClientBundle
{
    public static final HyperlinkResources INSTANCE = GWT.create(HyperlinkResources.class);

    @CssResource.NotStrict
    HyperlinkStyles hyperlinkStyles();

    public interface HyperlinkStyles extends CssResource
    {
        String helpPreview();

        String helpHyperlink();
        String helpTitle();
        String helpDescription();

        String code();
        String arbitraryCode();
        String warning();

        String xtermHyperlink();
        String xtermCommand();
        String xtermUnsupportedHyperlink();

        String helpTitlePanel();
        String helpTitlePanelPackage();
        String helpTitlePanelTopic();
    }
}
