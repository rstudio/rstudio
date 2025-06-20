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

    /**
     * Translated "Viewer".
     */
    @DefaultMessage("Viewer")
    @Key("viewerTitle")
    String viewerTitle();

    /**
     * Translated "Viewer Tab".
     */
    @DefaultMessage("Viewer Tab")
    @Key("viewerTabLabel")
    String viewerTabLabel();

    /**
     * Translated "Export".
     */
    @DefaultMessage("Export")
    @Key("exportText")
    String exportText();

    /**
     * Translated "Could Not Publish".
     */
    @DefaultMessage("Could Not Publish")
    @Key("couldNotPublishCaption")
    String couldNotPublishCaption();

    /**
     * Translated "Viewer Content".
     */
    @DefaultMessage("Viewer Content")
    @Key("viewerContentTitle")
    String viewerContentTitle();

    /**
     * Translated "Viewer Pane".
     */
    @DefaultMessage("Viewer Pane")
    @Key("viewerPaneTitle")
    String viewerPaneTitle();

    /**
     * Translated "Sync Editor".
     */
    @DefaultMessage("Sync Editor")
    @Key("syncEditorLabel")
    String syncEditorLabel();

    /**
     * Translated "Error".
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Preparing to export plot...".
     */
    @DefaultMessage("Preparing to export plot...")
    @Key("preparingToExportPlotMessage")
    String preparingToExportPlotMessage();

    /**
     * Translated "Saving standalone web pages".
     */
    @DefaultMessage("Saving standalone web pages")
    @Key("savingStandaloneWebPagesMessage")
    String savingStandaloneWebPagesMessage();

    /**
     * Translated "Save As Web Page".
     */
    @DefaultMessage("Save As Web Page")
    @Key("saveAsWebPageCaption")
    String saveAsWebPageCaption();

    /**
     * Translated "Saving as web page...".
     */
    @DefaultMessage("Saving as web page...")
    @Key("savingAsWebPageMessage")
    String savingAsWebPageMessage();

    /**
     * Translated "Clear Viewer".
     */
    @DefaultMessage("Clear Viewer")
    @Key("clearViewerCaption")
    String clearViewerCaption();

    /**
     * Translated "Are you sure you want to clear all of the items in the history?".
     */
    @DefaultMessage("Are you sure you want to clear all of the items in the history?")
    @Key("clearViewerMessage")
    String clearViewerMessage();

    /**
     * Translated "Clearing viewer...".
     */
    @DefaultMessage("Clearing viewer...")
    @Key("clearingViewerMessage")
    String clearingViewerMessage();

    /**
     * Translated "Viewer Pane Preview".
     */
    @DefaultMessage("Viewer Pane Preview")
    @Key("viewerPanePreviewTitle")
    String viewerPanePreviewTitle();

}
