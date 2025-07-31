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

    @DefaultMessage("Help")
    @Key("helpText")
    String helpText();

    @DefaultMessage("Help Pane")
    @Key("helpPaneTitle")
    String helpPaneTitle();

    @DefaultMessage("Help Tab")
    @Key("helpTabLabel")
    String helpTabLabel();

    @DefaultMessage("Help Tab Second")
    @Key("helpTabSecondLabel")
    String helpTabSecondLabel();

    @DefaultMessage("Find next (Enter)")
    @Key("findNextLabel")
    String findNextLabel();

    @DefaultMessage("Find previous")
    @Key("findPreviousLabel")
    String findPreviousLabel();

    @DefaultMessage("Find in Topic")
    @Key("findInTopicLabel")
    String findInTopicLabel();

    @DefaultMessage("No occurrences found")
    @Key("noOccurrencesFoundMessage")
    String noOccurrencesFoundMessage();

    @DefaultMessage("Search help")
    @Key("searchHelpLabel")
    String searchHelpLabel();

}
