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

    @DefaultMessage("Build")
    @Key("buildText")
    String buildText();

    @DefaultMessage("Build Tab")
    @Key("buildTabLabel")
    String buildTabLabel();

    @DefaultMessage("Build Book")
    @Key("buildBookText")
    String buildBookText();

    @DefaultMessage("Build Website")
    @Key("buildWebsiteText")
    String buildWebsiteText();

    @DefaultMessage("Build book options")
    @Key("buildBookOptionsText")
    String buildBookOptionsText();

    @DefaultMessage("Preview Book")
    @Key("serveBookText")
    String serveBookText();

    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    @DefaultMessage("All Formats")
    @Key("allFormatsLabel")
    String allFormatsLabel();

    @DefaultMessage("{0} Format")
    @Key("formatMenuLabel")
    String formatMenuLabel(String formatName);

    @DefaultMessage("Render Website")
    @Key("renderWebsiteText")
    String renderWebsiteText();

    @DefaultMessage("Render Book")
    @Key("renderBookText")
    String renderBookText();

    @DefaultMessage("Render Project")
    @Key("renderProjectText")
    String renderProjectText();

    @DefaultMessage("Building package documentation")
    @Key("packageDocumentationProgressCaption")
    String packageDocumentationProgressCaption();

    @DefaultMessage("Building sites")
    @Key("buildingSitesUserAction")
    String buildingSitesUserAction();

    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    @Key("terminalTerminatedQuestion")
    String terminalTerminatedQuestion();

    @DefaultMessage("Terminating Build...")
    @Key("terminatingBuildMessage")
    String terminatingBuildMessage();

    @DefaultMessage("Error Terminating Build")
    @Key("errorTerminatingBuildCaption")
    String errorTerminatingBuildCaption();

    @DefaultMessage("Unable to terminate build. Please try again.")
    @Key("errorTerminatingBuildMessage")
    String errorTerminatingBuildMessage();

    @DefaultMessage("Quarto Serve Error")
    @Key("quartoServeError")
    String quartoServeError();

    @DefaultMessage("Build All")
    @Key("buildAllLabel")
    String buildAllLabel();

    @DefaultMessage("Build all")
    @Key("buildAllDesc")
    String buildAllDesc();

    @DefaultMessage("Project")
    @Key("projectTypeText")
    String projectTypeText();

    @DefaultMessage("Book")
    @Key("bookText")
    String bookText();

    @DefaultMessage("Website")
    @Key("projectWebsiteText")
    String projectWebsiteText();

    @DefaultMessage("Render ")
    @Key("renderLabel")
    String renderLabel();

    @DefaultMessage("Preview ")
    @Key("serveLabel")
    String serveLabel();

}
