/*
 * ServerConstants.java
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

package org.rstudio.studio.client.server;

public interface ServerConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Error parsing results: {0}".
     *
     * @return translated "Error parsing results: {0}"
     */
    @DefaultMessage("Error parsing results: {0}")
    @Key("errorParsingResults")
    String errorParsingResults(String results);

    /**
     * Translated "(null)".
     *
     * @return translated "(null)"
     */
    @DefaultMessage("(null)")
    @Key("nullText")
    String nullText();

}
