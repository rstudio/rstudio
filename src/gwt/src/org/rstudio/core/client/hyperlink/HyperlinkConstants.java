/*
 * HyperlinkConstants.java
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
package org.rstudio.core.client.hyperlink;

public interface HyperlinkConstants extends com.google.gwt.i18n.client.Messages {
    

    @DefaultMessage("Documentation not found.")
    @Key("noDocumentation")
    String noDocumentation();

    @DefaultMessage("Package not found")
    @Key("noPackage")
    String noPackage();

    @DefaultMessage("Vignette not found.")
    @Key("noVignette")
    String noVignette();

    @DefaultMessage("click to run")
    @Key("clickToRun")
    String clickToRun();

    @DefaultMessage("No such file")
    @Key("noSuchFile")
    String noSuchFile();

    @DefaultMessage("''{0}'' does not exist.")
    @Key("doesNotExist")
    String doesNotExist(String filename);

}
