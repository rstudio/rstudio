/*
 * Presentation2Constants.java
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
package org.rstudio.studio.client.workbench.views.presentation2;

public interface Presentation2Constants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentationTitle")
    String presentationTitle();

    /**
     * Translated "Presentation Toolbar".
     *
     * @return translated "Presentation Toolbar"
     */
    @DefaultMessage("Presentation Toolbar")
    @Key("presentationToolbarLabel")
    String presentationToolbarLabel();

    /**
     * Translated "Present".
     *
     * @return translated "Present"
     */
    @DefaultMessage("Present")
    @Key("presentTitle")
    String presentTitle();

    /**
     * Translated "Presentation Slides Toolbar".
     *
     * @return translated "Presentation Slides Toolbar"
     */
    @DefaultMessage("Presentation Slides Toolbar")
    @Key("presentationSlidesToolbarLabel")
    String presentationSlidesToolbarLabel();

    /**
     * Translated "Presentation Preview".
     *
     * @return translated "Presentation Preview"
     */
    @DefaultMessage("Presentation Preview")
    @Key("presentationPreviewTitle")
    String presentationPreviewTitle();

}
