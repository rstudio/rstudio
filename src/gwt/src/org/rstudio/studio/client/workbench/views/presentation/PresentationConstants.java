/*
 * PresentationConstants.java
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
package org.rstudio.studio.client.workbench.views.presentation;

public interface PresentationConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Opening Presentation...".
     *
     * @return translated "Opening Presentation..."
     */
    @DefaultMessage("Opening Presentation...")
    String openingPresentationProgressMessage();

    /**
     * Translated "Unknown Presentation Command".
     *
     * @return translated "Unknown Presentation Command"
     */
    @DefaultMessage("Unknown Presentation Command")
    String unknownPresentationCommandCaption();

    /**
     * Translated "Unknown Console Directive".
     *
     * @return translated "Unknown Console Directive"
     */
    @DefaultMessage("Unknown Console Directive")
    String unknownConsoleDirectiveCaption();

    /**
     * Translated "Presentation Frame".
     *
     * @return translated "Presentation Frame"
     */
    @DefaultMessage("Presentation Frame")
    String presentationFrameTitle();

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    String presentationTitle();

    /**
     * Translated "Presentation Tab".
     *
     * @return translated "Presentation Tab"
     */
    @DefaultMessage("Presentation Tab")
    String presentationTabLabel();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    String moreText();

    /**
     * Translated "More presentation commands".
     *
     * @return translated "More presentation commands"
     */
    @DefaultMessage("More presentation commands")
    String morePresentationCommandsTitle();


    /**
     * Translated "Error Saving Presentation".
     *
     * @return translated "Error Saving Presentation"
     */
    @DefaultMessage("Error Saving Presentation")
    String errorSavingPresentationCaption();

    /**
     * Translated "Presentation:\n{0}".
     *
     * @return translated "Presentation:\n{0}"
     */
    @DefaultMessage("Presentation:\\n{0}")
    String presentationLabel(String title);

    /**
     * Translated "Save Presentation As".
     *
     * @return translated "Save Presentation As"
     */
    @DefaultMessage("Save Presentation As")
    String savePresentationAsCaption();

    /**
     * Translated "Saving Presentation...".
     *
     * @return translated "Saving Presentation..."
     */
    @DefaultMessage("Saving Presentation...")
    String savingPresentationProgressMessage();

    /**
     * Translated "Clear Knitr Cache".
     *
     * @return translated "Clear Knitr Cache"
     */
    @DefaultMessage("Clear Knitr Cache")
    String clearKnitrCacheCaption();

    /**
     * Translated "Clearing the Knitr cache will discard previously cached output and re-run all of the R code chunks within the presentation.\n\nAre you sure you want to clear the cache now?".
     *
     * @return translated "Clearing the Knitr cache will discard previously cached output and re-run all of the R code chunks within the presentation.\n\nAre you sure you want to clear the cache now?"
     */
    @DefaultMessage("Clearing the Knitr cache will discard previously cached output and re-run all of the R code chunks within the presentation.\\n\\nAre you sure you want to clear the cache now?")
    String clearKnitrCacheMessage();

    /**
     * Translated "Clearing Knitr Cache...".
     *
     * @return translated "Clearing Knitr Cache..."
     */
    @DefaultMessage("Clearing Knitr Cache...")
    String clearingKnitrCaption();

    /**
     * Translated "Error Clearing Cache".
     *
     * @return translated "Error Clearing Cache"
     */
    @DefaultMessage("Error Clearing Cache")
    String errorClearingCache();

    /**
     * Translated "Closing Presentation...".
     *
     * @return translated "Closing Presentation..."
     */
    @DefaultMessage("Closing Presentation...")
    String closingPresentationProgressMessage();

}
