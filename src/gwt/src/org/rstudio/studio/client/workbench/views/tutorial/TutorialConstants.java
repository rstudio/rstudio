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

    @Key("tutorialTitle")
    String tutorialTitle();

    @Key("errorLoadingTutorialCaption")
    String errorLoadingTutorialCaption();

    @Key("tutorialPaneTitle")
    String tutorialPaneTitle();

    @Key("tutorialTabLabel")
    String tutorialTabLabel();

    @Key("loadingTutorialProgressMessage")
    String loadingTutorialProgressMessage();

    @Key("errorInstallingLearnr")
    String errorInstallingLearnr();

    @Key("errorInstallingLearnrMessage")
    String errorInstallingLearnrMessage();

    @Key("installingLearnrCaption")
    String installingLearnrCaption();

    @Key("errorInstallingShiny")
    String errorInstallingShiny();

    @Key("errorInstallingShinyMessage")
    String errorInstallingShinyMessage();

    @Key("installingShinyCaption")
    String installingShinyCaption();

}
