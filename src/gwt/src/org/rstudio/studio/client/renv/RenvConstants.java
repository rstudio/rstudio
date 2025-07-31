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

    /**
     * Translate "Library".
     *
     * @return the translated value
     */
    @Key("libraryCaption")
    String libraryCaption();

    /**
     * Translate "Package".
     *
     * @return the translated value
     */
    @Key("packageColumnText")
    String packageColumnText();

    /**
     * Translate "Library Version".
     *
     * @return the translated value
     */
    @Key("libraryVersionColumnText")
    String libraryVersionColumnText();

    /**
     * Translate "Lockfile Version".
     *
     * @return the translated value
     */
    @Key("lockfileVersionColumnText")
    String lockfileVersionColumnText();

    /**
     * Translate "Action".
     *
     * @return the translated value
     */
    @Key("actionVersionColumnText")
    String actionVersionColumnText();

    /**
     * Translate "The following packages will be updated in the lockfile.".
     *
     * @return the translated value
     */
    @Key("snapshotHeaderLabel")
    String snapshotHeaderLabel();

    /**
     * Translate "The following changes will be made to the project library.".
     *
     * @return the translated value
     */
    @Key("restoreHeaderLabel")
    String restoreHeaderLabel();

    /**
     * Translate "[Not installed]".
     *
     * @return the translated value
     */
    @Key("libraryVersionNotInstalled")
    String libraryVersionNotInstalled();

    /**
     * Translate "[Not recorded]".
     *
     * @return the translated value
     */
    @Key("lockfileVersionNotRecorded")
    String lockfileVersionNotRecorded();

    /**
     * Translate "Add ''{0}'' [{1}] to the lockfile".
     *
     * @return the translated value
     */
    @Key("installAction")
    String installAction(String packageName, String libraryVersion);

    /**
     * Translate "Remove ''{0}'' [{1}] from the lockfile".
     *
     * @return the translated value
     */
    @Key("removeAction")
    String removeAction(String packageName, String lockfileVersion);

    /**
     * Translate "Update ''{0}'' [{1} -> {2}] in the lockfile".
     *
     * @return the translated value
     */
    @Key("updateAction")
    String updateAction(String packageName, String lockfileVersion, String libraryVersion);

    /**
     * Translate "Install ''{0}'' [{1}]".
     *
     * @return the translated value
     */
    @Key("restoreInstallAction")
    String restoreInstallAction(String packageName, String lockfileVersion);

    /**
     * Translate "Remove ''{0}'' [{1}]".
     *
     * @return the translated value
     */
    @Key("restoreRemoveAction")
    String restoreRemoveAction(String packageName, String libraryfileVersion);
}
