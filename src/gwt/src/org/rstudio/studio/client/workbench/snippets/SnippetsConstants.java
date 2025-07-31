/*
 * SnippetsConstants.java
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
package org.rstudio.studio.client.workbench.snippets;

public interface SnippetsConstants extends com.google.gwt.i18n.client.Messages {

    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    @DefaultMessage("Edit Snippets")
    @Key("editSnippetsText")
    String editSnippetsText();

    @DefaultMessage("Using Code Snippets")
    @Key("usingCodeSnippetsText")
    String usingCodeSnippetsText();

    @DefaultMessage("Error Applying Snippets ({0})")
    @Key("applyingSnippetsError")
    String applyingSnippetsError(String fileTypeLabel);
}
