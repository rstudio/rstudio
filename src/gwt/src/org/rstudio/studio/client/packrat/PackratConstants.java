/*
 * DataViewerConstants.java
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

package org.rstudio.studio.client.packrat;

import com.google.gwt.i18n.client.Constants;

public interface PackratConstants extends Constants {

    String packageColumnHeaderLabel();
    String packratColumnHeaderLabel();
    String libraryColumnHeaderLabel();
    String actionColumnHeaderLabel();
    String snapshotSummaryLabel();
    String restoreSummaryLabel();
    String packratResolveConflictDialogCaption();
    String okButtonCaption();
    String resolveConflictLabelText();
    String resolutionLabel();
    String snapshotChoiceRadioButtonLabel();
    String libraryChoiceRadioButton();
    String noSelectionMadeText();
    String noSelectionMadeMessage();
}
