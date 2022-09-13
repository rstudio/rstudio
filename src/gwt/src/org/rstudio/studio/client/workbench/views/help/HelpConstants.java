/*
 * HelpConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help;

public interface HelpConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    @Key("helpText")
    String helpText();

    /**
     * Translated "Help Pane".
     *
     * @return translated "Help Pane"
     */
    @DefaultMessage("Help Pane")
    @Key("helpPaneTitle")
    String helpPaneTitle();

    /**
     * Translated "Help Tab".
     *
     * @return translated "Help Tab"
     */
    @DefaultMessage("Help Tab")
    @Key("helpTabLabel")
    String helpTabLabel();

    /**
     * Translated "Help Tab Second".
     *
     * @return translated "Help Tab Second"
     */
    @DefaultMessage("Help Tab Second")
    @Key("helpTabSecondLabel")
    String helpTabSecondLabel();

    /**
     * Translated "Find next (Enter)".
     *
     * @return translated "Find next (Enter)"
     */
    @DefaultMessage("Find next (Enter)")
    @Key("findNextLabel")
    String findNextLabel();

    /**
     * Translated "Find previous".
     *
     * @return translated "Find previous"
     */
    @DefaultMessage("Find previous")
    @Key("findPreviousLabel")
    String findPreviousLabel();

    /**
     * Translated "Find in Topic".
     *
     * @return translated "Find in Topic"
     */
    @DefaultMessage("Find in Topic")
    @Key("findInTopicLabel")
    String findInTopicLabel();

    /**
     * Translated "No occurrences found".
     *
     * @return translated "No occurrences found"
     */
    @DefaultMessage("No occurrences found")
    @Key("noOccurrencesFoundMessage")
    String noOccurrencesFoundMessage();

    /**
     * Translated "Search help".
     *
     * @return translated "Search help"
     */
    @DefaultMessage("Search help")
    @Key("searchHelpLabel")
    String searchHelpLabel();

}
