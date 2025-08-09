/*
 * ViewBuildtoolsConstants.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

public interface ViewBuildtoolsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Build".
     *
     * @return translated "Build"
     */
    String buildText();

    /**
     * Translated "Build Tab".
     *
     * @return translated "Build Tab"
     */
    String buildTabLabel();

    /**
     * Translated "Build Book".
     *
     * @return translated "Build Book"
     */
    String buildBookText();

    /**
     * Translated "Build Website".
     *
     * @return translated "Build Website"
     */
    String buildWebsiteText();

    /**
     * Translated "Build book options".
     *
     * @return translated "Build book options"
     */
    String buildBookOptionsText();

    /**
     * Translated "Preview Book".
     *
     * @return translated "Preview Book"
     */
    String serveBookText();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    String moreText();

    /**
     * Translated "All Formats".
     *
     * @return translated "All Formats"
     */
    String allFormatsLabel();

    /**
     * Translated "{0} Format".
     *
     * @return translated "{0} Format"
     */
    String formatMenuLabel(String formatName);

    /**
     * Translated "Render Website".
     *
     * @return translated "Render Website"
     */
    String renderWebsiteText();

    /**
     * Translated "Render Book".
     *
     * @return translated "Render Book"
     */
    String renderBookText();

    /**
     * Translated "Render Project".
     *
     * @return translated "Render Project"
     */
    String renderProjectText();

    /**
     * Translated "Building package documentation".
     *
     * @return translated "Building package documentation"
     */
    String packageDocumentationProgressCaption();

    /**
     * Translated "Building sites".
     *
     * @return translated "Building sites"
     */
    String buildingSitesUserAction();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    String terminalTerminatedQuestion();

    /**
     * Translated "Terminating Build...".
     *
     * @return translated "Terminating Build..."
     */
    String terminatingBuildMessage();

    /**
     * Translated "Error Terminating Build".
     *
     * @return translated "Error Terminating Build"
     */
    String errorTerminatingBuildCaption();

    /**
     * Translated "Unable to terminate build. Please try again.".
     *
     * @return translated "Unable to terminate build. Please try again."
     */
    String errorTerminatingBuildMessage();

    /**
     * Translated "Quarto Serve Error".
     *
     * @return translated "Quarto Serve Error"
     */
    String quartoServeError();

    /**
     * Translated "Build All".
     *
     * @return translated "Build All"
     */
    String buildAllLabel();

    /**
     * Translated "Build all".
     *
     * @return translated "Build all"
     */
    String buildAllDesc();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    String projectTypeText();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    String bookText();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    String projectWebsiteText();

    /**
     * Translated "Render ".
     *
     * @return translated "Render "
     */
    String renderLabel();

    /**
     * Translated "Preview ".
     *
     * @return translated "Preview "
     */
    String serveLabel();

}
