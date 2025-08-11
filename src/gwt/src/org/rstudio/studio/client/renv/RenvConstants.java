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
package org.rstudio.studio.client.renv;
import com.google.gwt.i18n.client.Messages;

public interface RenvConstants extends Messages {
    String libraryCaption();
    String packageColumnText();
    String libraryVersionColumnText();
    String lockfileVersionColumnText();
    String actionVersionColumnText();
    String snapshotHeaderLabel();
    String restoreHeaderLabel();
    String libraryVersionNotInstalled();
    String lockfileVersionNotRecorded();
    String installAction(String packageName, String libraryVersion);
    String removeAction(String packageName, String lockfileVersion);
    String updateAction(String packageName, String lockfileVersion, String libraryVersion);
    String restoreInstallAction(String packageName, String lockfileVersion);
    String restoreRemoveAction(String packageName, String libraryfileVersion);
}
