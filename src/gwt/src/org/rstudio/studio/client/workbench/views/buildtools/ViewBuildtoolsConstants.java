/*
 * ViewBuildtoolsConstants.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

public interface ViewBuildtoolsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Build".
     *
     * @return translated "Build"
     */
    @DefaultMessage("Build")
    @Key("buildText")
    String buildText();

    /**
     * Translated "Build Tab".
     *
     * @return translated "Build Tab"
     */
    @DefaultMessage("Build Tab")
    @Key("buildTabLabel")
    String buildTabLabel();

    /**
     * Translated "Build Book".
     *
     * @return translated "Build Book"
     */
    @DefaultMessage("Build Book")
    @Key("buildBookText")
    String buildBookText();

    /**
     * Translated "Build Website".
     *
     * @return translated "Build Website"
     */
    @DefaultMessage("Build Website")
    @Key("buildWebsiteText")
    String buildWebsiteText();

    /**
     * Translated "Build book options".
     *
     * @return translated "Build book options"
     */
    @DefaultMessage("Build book options")
    @Key("buildBookOptionsText")
    String buildBookOptionsText();

    /**
     * Translated "Preview Book".
     *
     * @return translated "Preview Book"
     */
    @DefaultMessage("Preview Book")
    @Key("serveBookText")
    String serveBookText();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    /**
     * Translated "All Formats".
     *
     * @return translated "All Formats"
     */
    @DefaultMessage("All Formats")
    @Key("allFormatsLabel")
    String allFormatsLabel();

    /**
     * Translated "{0} Format".
     *
     * @return translated "{0} Format"
     */
    @DefaultMessage("{0} Format")
    @Key("formatMenuLabel")
    String formatMenuLabel(String formatName);

    /**
     * Translated "Render Website".
     *
     * @return translated "Render Website"
     */
    @DefaultMessage("Render Website")
    @Key("renderWebsiteText")
    String renderWebsiteText();

    /**
     * Translated "Render Book".
     *
     * @return translated "Render Book"
     */
    @DefaultMessage("Render Book")
    @Key("renderBookText")
    String renderBookText();

    /**
     * Translated "Render Project".
     *
     * @return translated "Render Project"
     */
    @DefaultMessage("Render Project")
    @Key("renderProjectText")
    String renderProjectText();

    /**
     * Translated "Building package documentation".
     *
     * @return translated "Building package documentation"
     */
    @DefaultMessage("Building package documentation")
    @Key("packageDocumentationProgressCaption")
    String packageDocumentationProgressCaption();

    /**
     * Translated "Building sites".
     *
     * @return translated "Building sites"
     */
    @DefaultMessage("Building sites")
    @Key("buildingSitesUserAction")
    String buildingSitesUserAction();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    @Key("terminalTerminatedQuestion")
    String terminalTerminatedQuestion();

    /**
     * Translated "Terminating Build...".
     *
     * @return translated "Terminating Build..."
     */
    @DefaultMessage("Terminating Build...")
    @Key("terminatingBuildMessage")
    String terminatingBuildMessage();

    /**
     * Translated "Error Terminating Build".
     *
     * @return translated "Error Terminating Build"
     */
    @DefaultMessage("Error Terminating Build")
    @Key("errorTerminatingBuildCaption")
    String errorTerminatingBuildCaption();

    /**
     * Translated "Unable to terminate build. Please try again.".
     *
     * @return translated "Unable to terminate build. Please try again."
     */
    @DefaultMessage("Unable to terminate build. Please try again.")
    @Key("errorTerminatingBuildMessage")
    String errorTerminatingBuildMessage();

    /**
     * Translated "Quarto Serve Error".
     *
     * @return translated "Quarto Serve Error"
     */
    @DefaultMessage("Quarto Serve Error")
    @Key("quartoServeError")
    String quartoServeError();

    /**
     * Translated "Build All".
     *
     * @return translated "Build All"
     */
    @DefaultMessage("Build All")
    @Key("buildAllLabel")
    String buildAllLabel();

    /**
     * Translated "Build all".
     *
     * @return translated "Build all"
     */
    @DefaultMessage("Build all")
    @Key("buildAllDesc")
    String buildAllDesc();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    @DefaultMessage("Project")
    @Key("projectTypeText")
    String projectTypeText();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    @DefaultMessage("Book")
    @Key("bookText")
    String bookText();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    @Key("projectWebsiteText")
    String projectWebsiteText();

    /**
     * Translated "Render ".
     *
     * @return translated "Render "
     */
    @DefaultMessage("Render ")
    @Key("renderLabel")
    String renderLabel();

    /**
     * Translated "Preview ".
     *
     * @return translated "Preview "
     */
    @DefaultMessage("Preview ")
    @Key("serveLabel")
    String serveLabel();


}
