/*
 * HelpConstants.java
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
package org.rstudio.studio.client.workbench.views.help;

public interface HelpConstants extends com.google.gwt.i18n.client.Messages {

    @Key("helpText")
    String helpText();

    @Key("helpPaneTitle")
    String helpPaneTitle();

    @Key("helpTabLabel")
    String helpTabLabel();

    @Key("helpTabSecondLabel")
    String helpTabSecondLabel();

    @Key("findNextLabel")
    String findNextLabel();

    @Key("findPreviousLabel")
    String findPreviousLabel();

    @Key("findInTopicLabel")
    String findInTopicLabel();

    @Key("noOccurrencesFoundMessage")
    String noOccurrencesFoundMessage();

    @Key("searchHelpLabel")
    String searchHelpLabel();

}
