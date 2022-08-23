/*
 * HyperlinkConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.hyperlink;

public interface HyperlinkConstants extends com.google.gwt.i18n.client.Messages {
    
    /**
     * Translated "Documentation not found.".
     *
     * @return translated "Documentation not found."
     */
    @DefaultMessage("Documentation not found.")
    @Key("noDocumentation")
    String noDocumentation();

    /**
     * Translated "Package not found.".
     *
     * @return translated "Package not found."
     */
    @DefaultMessage("Package not found")
    @Key("noPackage")
    String noPackage();

    /**
     * Translated "Vignette not found.".
     *
     * @return translated "Vignette not found."
     */
    @DefaultMessage("Vignette not found.")
    @Key("noVignette")
    String noVignette();

    /**
     * Translated "click to run".
     *
     * @return translated "click to run"
     */
    @DefaultMessage("click to run")
    @Key("clickToRun")
    String clickToRun();

    /**
     * Translated "No such file".
     *
     * @return translated "No such file"
     */
    @DefaultMessage("No such file")
    @Key("noSuchFile")
    String noSuchFile();

    /**
     * Translated "''{0}'' does not exist.".
     *
     * @return translated "''{0}'' does not exist."
     */
    @DefaultMessage("''{0}'' does not exist.")
    @Key("doesNotExist")
    String doesNotExist(String filename);

}
