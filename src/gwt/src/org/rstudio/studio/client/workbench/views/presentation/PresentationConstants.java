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

    @DefaultMessage("Opening Presentation...")
    @Key("openingPresentationProgressMessage")
    String openingPresentationProgressMessage();

    @DefaultMessage("Unknown Presentation Command")
    @Key("unknownPresentationCommandCaption")
    String unknownPresentationCommandCaption();

    @DefaultMessage("Unknown Console Directive")
    @Key("unknownConsoleDirectiveCaption")
    String unknownConsoleDirectiveCaption();

    @DefaultMessage("Presentation Frame")
    @Key("presentationFrameTitle")
    String presentationFrameTitle();

    @DefaultMessage("Presentation")
    @Key("presentationTitle")
    String presentationTitle();

    @DefaultMessage("Presentation Tab")
    @Key("presentationTabLabel")
    String presentationTabLabel();

    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    @DefaultMessage("More presentation commands")
    @Key("morePresentationCommandsTitle")
    String morePresentationCommandsTitle();

    @DefaultMessage("Error Saving Presentation")
    @Key("errorSavingPresentationCaption")
    String errorSavingPresentationCaption();

    @DefaultMessage("Presentation:\\n{0}")
    @Key("presentationLabel")
    String presentationLabel(String title);

    @DefaultMessage("Save Presentation As")
    @Key("savePresentationAsCaption")
    String savePresentationAsCaption();

    @DefaultMessage("Saving Presentation...")
    @Key("savingPresentationProgressMessage")
    String savingPresentationProgressMessage();

    @DefaultMessage("Clear Knitr Cache")
    @Key("clearKnitrCacheCaption")
    String clearKnitrCacheCaption();

    @DefaultMessage("Clearing the Knitr cache will discard previously cached output and re-run all of the R code chunks within the presentation.\\n\\nAre you sure you want to clear the cache now?")
    @Key("clearKnitrCacheMessage")
    String clearKnitrCacheMessage();

    @DefaultMessage("Clearing Knitr Cache...")
    @Key("clearingKnitrCaption")
    String clearingKnitrCaption();

    @DefaultMessage("Error Clearing Cache")
    @Key("errorClearingCache")
    String errorClearingCache();

    @DefaultMessage("Closing Presentation...")
    @Key("closingPresentationProgressMessage")
    String closingPresentationProgressMessage();

}
