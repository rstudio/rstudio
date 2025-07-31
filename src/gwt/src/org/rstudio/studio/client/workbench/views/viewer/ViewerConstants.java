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

    @DefaultMessage("Viewer")
    @Key("viewerTitle")
    String viewerTitle();

    @DefaultMessage("Viewer Tab")
    @Key("viewerTabLabel")
    String viewerTabLabel();

    @DefaultMessage("Export")
    @Key("exportText")
    String exportText();

    @DefaultMessage("Could Not Publish")
    @Key("couldNotPublishCaption")
    String couldNotPublishCaption();

    @DefaultMessage("Viewer Content")
    @Key("viewerContentTitle")
    String viewerContentTitle();

    @DefaultMessage("Viewer Pane")
    @Key("viewerPaneTitle")
    String viewerPaneTitle();

    @DefaultMessage("Sync Editor")
    @Key("syncEditorLabel")
    String syncEditorLabel();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("Preparing to export plot...")
    @Key("preparingToExportPlotMessage")
    String preparingToExportPlotMessage();

    @DefaultMessage("Saving standalone web pages")
    @Key("savingStandaloneWebPagesMessage")
    String savingStandaloneWebPagesMessage();

    @DefaultMessage("Save As Web Page")
    @Key("saveAsWebPageCaption")
    String saveAsWebPageCaption();

    @DefaultMessage("Saving as web page...")
    @Key("savingAsWebPageMessage")
    String savingAsWebPageMessage();

    @DefaultMessage("Clear Viewer")
    @Key("clearViewerCaption")
    String clearViewerCaption();

    @DefaultMessage("Are you sure you want to clear all of the items in the history?")
    @Key("clearViewerMessage")
    String clearViewerMessage();

    @DefaultMessage("Clearing viewer...")
    @Key("clearingViewerMessage")
    String clearingViewerMessage();

    @DefaultMessage("Viewer Pane Preview")
    @Key("viewerPanePreviewTitle")
    String viewerPanePreviewTitle();

}
