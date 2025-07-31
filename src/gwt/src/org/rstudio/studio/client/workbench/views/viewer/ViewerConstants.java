/*
 * ViewerConstants.java
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
package org.rstudio.studio.client.workbench.views.viewer;

public interface ViewerConstants extends com.google.gwt.i18n.client.Messages {

    @Key("viewerTitle")
    String viewerTitle();

    @Key("viewerTabLabel")
    String viewerTabLabel();

    @Key("exportText")
    String exportText();

    @Key("couldNotPublishCaption")
    String couldNotPublishCaption();

    @Key("viewerContentTitle")
    String viewerContentTitle();

    @Key("viewerPaneTitle")
    String viewerPaneTitle();

    @Key("syncEditorLabel")
    String syncEditorLabel();

    @Key("errorCaption")
    String errorCaption();

    @Key("preparingToExportPlotMessage")
    String preparingToExportPlotMessage();

    @Key("savingStandaloneWebPagesMessage")
    String savingStandaloneWebPagesMessage();

    @Key("saveAsWebPageCaption")
    String saveAsWebPageCaption();

    @Key("savingAsWebPageMessage")
    String savingAsWebPageMessage();

    @Key("clearViewerCaption")
    String clearViewerCaption();

    @Key("clearViewerMessage")
    String clearViewerMessage();

    @Key("clearingViewerMessage")
    String clearingViewerMessage();

    @Key("viewerPanePreviewTitle")
    String viewerPanePreviewTitle();

}
