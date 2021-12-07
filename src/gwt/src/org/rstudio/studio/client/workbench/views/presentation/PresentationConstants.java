/*
 * PresentationConstants.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.presentation;

public interface PresentationConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Opening Presentation...".
     *
     * @return translated "Opening Presentation..."
     */
    @DefaultMessage("Opening Presentation...")
    @Key("openingPresentationProgressMessage")
    String openingPresentationProgressMessage();

    /**
     * Translated "Unknown Presentation Command".
     *
     * @return translated "Unknown Presentation Command"
     */
    @DefaultMessage("Unknown Presentation Command")
    @Key("unknownPresentationCommandCaption")
    String unknownPresentationCommandCaption();

    /**
     * Translated "Unknown Console Directive".
     *
     * @return translated "Unknown Console Directive"
     */
    @DefaultMessage("Unknown Console Directive")
    @Key("unknownConsoleDirectiveCaption")
    String unknownConsoleDirectiveCaption();

    /**
     * Translated "Presentation Frame".
     *
     * @return translated "Presentation Frame"
     */
    @DefaultMessage("Presentation Frame")
    @Key("presentationFrameTitle")
    String presentationFrameTitle();

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentationTitle")
    String presentationTitle();

    /**
     * Translated "Presentation Tab".
     *
     * @return translated "Presentation Tab"
     */
    @DefaultMessage("Presentation Tab")
    @Key("presentationTabLabel")
    String presentationTabLabel();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    /**
     * Translated "More presentation commands".
     *
     * @return translated "More presentation commands"
     */
    @DefaultMessage("More presentation commands")
    @Key("morePresentationCommandsTitle")
    String morePresentationCommandsTitle();


    /**
     * Translated "Error Saving Presentation".
     *
     * @return translated "Error Saving Presentation"
     */
    @DefaultMessage("Error Saving Presentation")
    @Key("errorSavingPresentationCaption")
    String errorSavingPresentationCaption();

    /**
     * Translated "Presentation:\n{0}".
     *
     * @return translated "Presentation:\n{0}"
     */
    @DefaultMessage("Presentation:\\n{0}")
    @Key("presentationLabel")
    String presentationLabel(String title);

}
