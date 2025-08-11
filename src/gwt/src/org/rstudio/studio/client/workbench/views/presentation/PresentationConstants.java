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
    String openingPresentationProgressMessage();
    String unknownPresentationCommandCaption();
    String unknownConsoleDirectiveCaption();
    String presentationFrameTitle();
    String presentationTitle();
    String presentationTabLabel();
    String moreText();
    String morePresentationCommandsTitle();
    String errorSavingPresentationCaption();
    String presentationLabel(String title);
    String savePresentationAsCaption();
    String savingPresentationProgressMessage();
    String clearKnitrCacheCaption();
    String clearKnitrCacheMessage();
    String clearingKnitrCaption();
    String errorClearingCache();
    String closingPresentationProgressMessage();
}
