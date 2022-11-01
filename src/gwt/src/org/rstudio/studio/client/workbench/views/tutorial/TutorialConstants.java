/*
 * TutorialConstants.java
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
package org.rstudio.studio.client.workbench.views.tutorial;

public interface TutorialConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Tutorial".
     *
     * @return translated "Tutorial"
     */
    @DefaultMessage("Tutorial")
    @Key("tutorialTitle")
    String tutorialTitle();

    /**
     * Translated "Error Loading Tutorial".
     *
     * @return translated "Error Loading Tutorial"
     */
    @DefaultMessage("Error Loading Tutorial")
    @Key("errorLoadingTutorialCaption")
    String errorLoadingTutorialCaption();

    /**
     * Translated "Tutorial Pane".
     *
     * @return translated "Tutorial Pane"
     */
    @DefaultMessage("Tutorial Pane")
    @Key("tutorialPaneTitle")
    String tutorialPaneTitle();

    /**
     * Translated "Tutorial Tab".
     *
     * @return translated "Tutorial Tab"
     */
    @DefaultMessage("Tutorial Tab")
    @Key("tutorialTabLabel")
    String tutorialTabLabel();

    /**
     * Translated "Loading tutorial...".
     *
     * @return translated "Loading tutorial..."
     */
    @DefaultMessage("Loading tutorial...")
    @Key("loadingTutorialProgressMessage")
    String loadingTutorialProgressMessage();

    /**
     * Translated "Error installing learnr".
     *
     * @return translated "Error installing learnr"
     */
    @DefaultMessage("Error installing learnr")
    @Key("errorInstallingLearnr")
    String errorInstallingLearnr();

    /**
     * Translated "RStudio was unable to install the learnr package.".
     *
     * @return translated "RStudio was unable to install the learnr package."
     */
    @DefaultMessage("RStudio was unable to install the learnr package.")
    @Key("errorInstallingLearnrMessage")
    String errorInstallingLearnrMessage();

    /**
     * Translated "Installing learnr...".
     *
     * @return translated "Installing learnr..."
     */
    @DefaultMessage("Installing learnr...")
    @Key("installingLearnrCaption")
    String installingLearnrCaption();

    /**
     * Translated "Error installing shiny".
     *
     * @return translated "Error installing shiny"
     */
    @DefaultMessage("Error installing shiny")
    @Key("errorInstallingShiny")
    String errorInstallingShiny();

    /**
     * Translated "RStudio was unable to install the shiny package.".
     *
     * @return translated "RStudio was unable to install the shiny package."
     */
    @DefaultMessage("RStudio was unable to install the shiny package.")
    @Key("errorInstallingShinyMessage")
    String errorInstallingShinyMessage();

    /**
     * Translated "Installing shiny...".
     *
     * @return translated "Installing shiny..."
     */
    @DefaultMessage("Installing shiny...")
    @Key("installingShinyCaption")
    String installingShinyCaption();

}
