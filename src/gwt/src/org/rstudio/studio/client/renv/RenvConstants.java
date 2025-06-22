/*
 * RenvConstants.java
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

    /**
     * Translate "Library".
     */
    @DefaultMessage("Library")
    @Key("libraryCaption")
    String libraryCaption();

    /**
     * Translate "Package".
     */
    @DefaultMessage("Package")
    @Key("packageColumnText")
    String packageColumnText();

    /**
     * Translate "Library Version".
     */
    @DefaultMessage("Library Version")
    @Key("libraryVersionColumnText")
    String libraryVersionColumnText();

    /**
     * Translate "Lockfile Version".
     */
    @DefaultMessage("Lockfile Version")
    @Key("lockfileVersionColumnText")
    String lockfileVersionColumnText();

    /**
     * Translate "Action".
     */
    @DefaultMessage("Action")
    @Key("actionVersionColumnText")
    String actionVersionColumnText();

    /**
     * Translate "The following packages will be updated in the lockfile.".
     */
    @DefaultMessage("The following packages will be updated in the lockfile.")
    @Key("snapshotHeaderLabel")
    String snapshotHeaderLabel();

    /**
     * Translate "The following changes will be made to the project library.".
     */
    @DefaultMessage("The following changes will be made to the project library.")
    @Key("restoreHeaderLabel")
    String restoreHeaderLabel();

    /**
     * Translate "[Not installed]".
     */
    @DefaultMessage("[Not installed]")
    @Key("libraryVersionNotInstalled")
    String libraryVersionNotInstalled();

    /**
     * Translate "[Not recorded]".
     */
    @DefaultMessage("[Not recorded]")
    @Key("lockfileVersionNotRecorded")
    String lockfileVersionNotRecorded();

    /**
     * Translate "Add {0} {1} to the lockfile".
     */
    @DefaultMessage("Add {0} {1} to the lockfile")
    @Key("installAction")
    String installAction(String packageName, String libraryVersion);

    /**
     * Translate "Remove {0} {1} from the lockfile".
     */
    @DefaultMessage("Remove {0} {1} from the lockfile")
    @Key("removeAction")
    String removeAction(String packageName, String lockfileVersion);

    /**
     * Translate "Update {0} [{1} -> {2}] in the lockfile".
     */
    @DefaultMessage("Update {0} [{1} -> {2}] in the lockfile")
    @Key("updateAction")
    String updateAction(String packageName, String lockfileVersion, String libraryVersion);

    /**
     * Translate "Install {0} {1}".
     */
    @DefaultMessage("Install {0} {1}")
    @Key("restoreInstallAction")
    String restoreInstallAction(String packageName, String lockfileVersion);

    /**
     * Translate "Remove {0} {1}".
     */
    @DefaultMessage("Remove {0} {1}")
    @Key("restoreRemoveAction")
    String restoreRemoveAction(String packageName, String libraryfileVersion);
}
