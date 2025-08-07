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
     *
     * @return translated "Viewer"
     */
    @DefaultMessage("Viewer")
    String viewerTitle();

    /**
     * Translated "Viewer Tab".
     *
     * @return translated "Viewer Tab"
     */
    @DefaultMessage("Viewer Tab")
    String viewerTabLabel();

    /**
     * Translated "Export".
     *
     * @return translated "Export"
     */
    @DefaultMessage("Export")
    String exportText();

    /**
     * Translated "Could Not Publish".
     *
     * @return translated "Could Not Publish"
     */
    @DefaultMessage("Could Not Publish")
    String couldNotPublishCaption();

    /**
     * Translated "Viewer Content".
     *
     * @return translated "Viewer Content"
     */
    @DefaultMessage("Viewer Content")
    String viewerContentTitle();

    /**
     * Translated "Viewer Pane".
     *
     * @return translated "Viewer Pane"
     */
    @DefaultMessage("Viewer Pane")
    String viewerPaneTitle();

    /**
     * Translated "Sync Editor".
     *
     * @return translated "Sync Editor"
     */
    @DefaultMessage("Sync Editor")
    String syncEditorLabel();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCaption();

    /**
     * Translated "Preparing to export plot...".
     *
     * @return translated "Preparing to export plot..."
     */
    @DefaultMessage("Preparing to export plot...")
    String preparingToExportPlotMessage();

    /**
     * Translated "Saving standalone web pages".
     *
     * @return translated "Saving standalone web pages"
     */
    @DefaultMessage("Saving standalone web pages")
    String savingStandaloneWebPagesMessage();

    /**
     * Translated "Save As Web Page".
     *
     * @return translated "Save As Web Page"
     */
    @DefaultMessage("Save As Web Page")
    String saveAsWebPageCaption();

    /**
     * Translated "Saving as web page...".
     *
     * @return translated "Saving as web page..."
     */
    @DefaultMessage("Saving as web page...")
    String savingAsWebPageMessage();

    /**
     * Translated "Clear Viewer".
     *
     * @return translated "Clear Viewer"
     */
    @DefaultMessage("Clear Viewer")
    String clearViewerCaption();

    /**
     * Translated "Are you sure you want to clear all of the items in the history?".
     *
     * @return translated "Are you sure you want to clear all of the items in the history?"
     */
    @DefaultMessage("Are you sure you want to clear all of the items in the history?")
    String clearViewerMessage();

    /**
     * Translated "Clearing viewer...".
     *
     * @return translated "Clearing viewer..."
     */
    @DefaultMessage("Clearing viewer...")
    String clearingViewerMessage();

    /**
     * Translated "Viewer Pane Preview".
     *
     * @return translated "Viewer Pane Preview"
     */
    @DefaultMessage("Viewer Pane Preview")
    String viewerPanePreviewTitle();

}
