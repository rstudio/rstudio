/*
 * DataViewerConstants.java
 *
 * Copyright (C) 2022 by Posit, PBC
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

package org.rstudio.studio.client.packrat;

import com.google.gwt.i18n.client.Constants;

public interface PackratConstants extends Constants {

    /**
     * Translate "Package".
     *
     * @return the translated value
     */
    @DefaultStringValue("Package")
    @Key("packageColumnHeaderLabel")
    String packageColumnHeaderLabel();

    /**
     * Translate "Packrat".
     *
     * @return the translated value
     */
    @DefaultStringValue("Packrat")
    @Key("packratColumnHeaderLabel")
    String packratColumnHeaderLabel();

    /**
     * Translate "Library".
     *
     * @return the translated value
     */
    @DefaultStringValue("Library")
    @Key("libraryColumnHeaderLabel")
    String libraryColumnHeaderLabel();

    /**
     * Translate "Action".
     *
     * @return the translated value
     */
    @DefaultStringValue("Action")
    @Key("actionColumnHeaderLabel")
    String actionColumnHeaderLabel();

    /**
     * Translate "The following packages have changed in " +
     *                "your project's private library. Select Snapshot to save " +
     *                "these changes in Packrat.".
     *
     * @return the translated value
     */
    @DefaultStringValue("The following packages have changed in your project's private library. Select Snapshot to save these changes in Packrat.")
    @Key("snapshotSummaryLabel")
    String snapshotSummaryLabel();

    /**
     * Translate "The following packages have changed in " +
     *                "Packrat. Select Restore to apply these changes to your " +
     *                "project's private library.".
     *
     * @return the translated value
     */
    @DefaultStringValue("The following packages have changed in Packrat. Select Restore to apply these changes to your project's private library.")
    @Key("restoreSummaryLabel")
    String restoreSummaryLabel();

    /**
     * Translate "Resolve Conflict".
     *
     * @return the translated value
     */
    @DefaultStringValue("Resolve Conflict")
    @Key("packratResolveConflictDialogCaption")
    String packratResolveConflictDialogCaption();

    /**
     * Translate "Resolve".
     *
     * @return the translated value
     */
    @DefaultStringValue("Resolve")
    @Key("okButtonCaption")
    String okButtonCaption();

    /**
     * Translate "Packrat's packages are out of sync with the packages currently " +
     *         "installed in your library. To resolve the conflict you need to " +
     *         "either update Packrat to match your library or update your library " +
     *         "to match Packrat.".
     *
     * @return the translated value
     */
    @DefaultStringValue("Packrat's packages are out of sync with the packages currently installed in your library. To resolve the conflict you need to either update Packrat to match your library or update your library to match Packrat.")
    @Key("resolveConflictLabelText")
    String resolveConflictLabelText();

    /**
     * Translate "Resolution:".
     *
     * @return the translated value
     */
    @DefaultStringValue("Resolution:")
    @Key("resolutionLabel")
    String resolutionLabel();

    /**
     * Translate "Update Packrat (Snapshot)".
     *
     * @return the translated value
     */
    @DefaultStringValue("Update Packrat (Snapshot)")
    @Key("snapshotChoiceRadioButtonLabel")
    String snapshotChoiceRadioButtonLabel();

    /**
     * Translate "Update Library (Restore)".
     *
     * @return the translated value
     */
    @DefaultStringValue("Update Library (Restore)")
    @Key("libraryChoiceRadioButton")
    String libraryChoiceRadioButton();

    /**
     * Translate "No Selection Made".
     *
     * @return the translated value
     */
    @DefaultStringValue("No Selection Made")
    @Key("noSelectionMadeText")
    String noSelectionMadeText();

    /**
     * Translate "You must choose to either update Packrat (snapshot) or " +
     *                "update the project's private library (restore).".
     *
     * @return the translated value
     */
    @DefaultStringValue("You must choose to either update Packrat (snapshot) or update the project's private library (restore).")
    @Key("noSelectionMadeMessage")
    String noSelectionMadeMessage();

}
