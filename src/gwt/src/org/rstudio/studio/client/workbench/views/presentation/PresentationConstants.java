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

    @Key("openingPresentationProgressMessage")
    String openingPresentationProgressMessage();

    @Key("unknownPresentationCommandCaption")
    String unknownPresentationCommandCaption();

    @Key("unknownConsoleDirectiveCaption")
    String unknownConsoleDirectiveCaption();

    @Key("presentationFrameTitle")
    String presentationFrameTitle();

    @Key("presentationTitle")
    String presentationTitle();

    @Key("presentationTabLabel")
    String presentationTabLabel();

    @Key("moreText")
    String moreText();

    @Key("morePresentationCommandsTitle")
    String morePresentationCommandsTitle();

    @Key("errorSavingPresentationCaption")
    String errorSavingPresentationCaption();

    @Key("presentationLabel")
    String presentationLabel(String title);

    @Key("savePresentationAsCaption")
    String savePresentationAsCaption();

    @Key("savingPresentationProgressMessage")
    String savingPresentationProgressMessage();

    @Key("clearKnitrCacheCaption")
    String clearKnitrCacheCaption();

    @Key("clearKnitrCacheMessage")
    String clearKnitrCacheMessage();

    @Key("clearingKnitrCaption")
    String clearingKnitrCaption();

    @Key("errorClearingCache")
    String errorClearingCache();

    @Key("closingPresentationProgressMessage")
    String closingPresentationProgressMessage();

}
