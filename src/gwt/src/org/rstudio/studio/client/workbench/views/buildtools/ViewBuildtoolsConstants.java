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
    String buildText();
    String buildTabLabel();
    String buildBookText();
    String buildWebsiteText();
    String buildBookOptionsText();
    String serveBookText();
    String moreText();
    String allFormatsLabel();
    String formatMenuLabel(String formatName);
    String renderWebsiteText();
    String renderBookText();
    String renderProjectText();
    String packageDocumentationProgressCaption();
    String buildingSitesUserAction();
    String terminalTerminatedQuestion();
    String terminatingBuildMessage();
    String errorTerminatingBuildCaption();
    String errorTerminatingBuildMessage();
    String quartoServeError();
    String buildAllLabel();
    String buildAllDesc();
    String projectTypeText();
    String bookText();
    String projectWebsiteText();
    String renderLabel();
    String serveLabel();
}
