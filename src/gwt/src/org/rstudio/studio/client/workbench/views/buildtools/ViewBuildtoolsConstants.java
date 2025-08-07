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
    @DefaultMessage("Build")
    String buildText();

    /**
     * Translated "Build Tab".
     *
     * @return translated "Build Tab"
     */
    @DefaultMessage("Build Tab")
    String buildTabLabel();

    /**
     * Translated "Build Book".
     *
     * @return translated "Build Book"
     */
    @DefaultMessage("Build Book")
    String buildBookText();

    /**
     * Translated "Build Website".
     *
     * @return translated "Build Website"
     */
    @DefaultMessage("Build Website")
    String buildWebsiteText();

    /**
     * Translated "Build book options".
     *
     * @return translated "Build book options"
     */
    @DefaultMessage("Build book options")
    String buildBookOptionsText();

    /**
     * Translated "Preview Book".
     *
     * @return translated "Preview Book"
     */
    @DefaultMessage("Preview Book")
    String serveBookText();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    String moreText();

    /**
     * Translated "All Formats".
     *
     * @return translated "All Formats"
     */
    @DefaultMessage("All Formats")
    String allFormatsLabel();

    /**
     * Translated "{0} Format".
     *
     * @return translated "{0} Format"
     */
    @DefaultMessage("{0} Format")
    String formatMenuLabel(String formatName);

    /**
     * Translated "Render Website".
     *
     * @return translated "Render Website"
     */
    @DefaultMessage("Render Website")
    String renderWebsiteText();

    /**
     * Translated "Render Book".
     *
     * @return translated "Render Book"
     */
    @DefaultMessage("Render Book")
    String renderBookText();

    /**
     * Translated "Render Project".
     *
     * @return translated "Render Project"
     */
    @DefaultMessage("Render Project")
    String renderProjectText();

    /**
     * Translated "Building package documentation".
     *
     * @return translated "Building package documentation"
     */
    @DefaultMessage("Building package documentation")
    String packageDocumentationProgressCaption();

    /**
     * Translated "Building sites".
     *
     * @return translated "Building sites"
     */
    @DefaultMessage("Building sites")
    String buildingSitesUserAction();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    String terminalTerminatedQuestion();

    /**
     * Translated "Terminating Build...".
     *
     * @return translated "Terminating Build..."
     */
    @DefaultMessage("Terminating Build...")
    String terminatingBuildMessage();

    /**
     * Translated "Error Terminating Build".
     *
     * @return translated "Error Terminating Build"
     */
    @DefaultMessage("Error Terminating Build")
    String errorTerminatingBuildCaption();

    /**
     * Translated "Unable to terminate build. Please try again.".
     *
     * @return translated "Unable to terminate build. Please try again."
     */
    @DefaultMessage("Unable to terminate build. Please try again.")
    String errorTerminatingBuildMessage();

    /**
     * Translated "Quarto Serve Error".
     *
     * @return translated "Quarto Serve Error"
     */
    @DefaultMessage("Quarto Serve Error")
    String quartoServeError();

    /**
     * Translated "Build All".
     *
     * @return translated "Build All"
     */
    @DefaultMessage("Build All")
    String buildAllLabel();

    /**
     * Translated "Build all".
     *
     * @return translated "Build all"
     */
    @DefaultMessage("Build all")
    String buildAllDesc();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    @DefaultMessage("Project")
    String projectTypeText();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    @DefaultMessage("Book")
    String bookText();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    String projectWebsiteText();

    /**
     * Translated "Render ".
     *
     * @return translated "Render "
     */
    @DefaultMessage("Render ")
    String renderLabel();

    /**
     * Translated "Preview ".
     *
     * @return translated "Preview "
     */
    @DefaultMessage("Preview ")
    String serveLabel();

}
