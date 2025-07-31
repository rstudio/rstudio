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

    @Key("buildText")
    String buildText();

    @Key("buildTabLabel")
    String buildTabLabel();

    @Key("buildBookText")
    String buildBookText();

    @Key("buildWebsiteText")
    String buildWebsiteText();

    @Key("buildBookOptionsText")
    String buildBookOptionsText();

    @Key("serveBookText")
    String serveBookText();

    @Key("moreText")
    String moreText();

    @Key("allFormatsLabel")
    String allFormatsLabel();

    @Key("formatMenuLabel")
    String formatMenuLabel(String formatName);

    @Key("renderWebsiteText")
    String renderWebsiteText();

    @Key("renderBookText")
    String renderBookText();

    @Key("renderProjectText")
    String renderProjectText();

    @Key("packageDocumentationProgressCaption")
    String packageDocumentationProgressCaption();

    @Key("buildingSitesUserAction")
    String buildingSitesUserAction();

    @Key("terminalTerminatedQuestion")
    String terminalTerminatedQuestion();

    @Key("terminatingBuildMessage")
    String terminatingBuildMessage();

    @Key("errorTerminatingBuildCaption")
    String errorTerminatingBuildCaption();

    @Key("errorTerminatingBuildMessage")
    String errorTerminatingBuildMessage();

    @Key("quartoServeError")
    String quartoServeError();

    @Key("buildAllLabel")
    String buildAllLabel();

    @Key("buildAllDesc")
    String buildAllDesc();

    @Key("projectTypeText")
    String projectTypeText();

    @Key("bookText")
    String bookText();

    @Key("projectWebsiteText")
    String projectWebsiteText();

    @Key("renderLabel")
    String renderLabel();

    @Key("serveLabel")
    String serveLabel();

}
