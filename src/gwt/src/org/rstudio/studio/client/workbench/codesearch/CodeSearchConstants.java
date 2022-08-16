/*
 * CodeSearchConstants.java
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
package org.rstudio.studio.client.workbench.codesearch;

public interface CodeSearchConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Code Search Error".
     *
     * @return translated "Code Search Error"
     */
    @DefaultMessage("Code Search Error")
    @Key("codeSearchError")
    String codeSearchError();

    /**
     * Translated "Go to File/Function".
     *
     * @return translated "Go to File/Function"
     */
    @DefaultMessage("Go to File/Function")
    @Key("fileFunctionLabel")
    String fileFunctionLabel();

    /**
     * Translated "Filter by file or function name".
     *
     * @return translated "Filter by file or function name"
     */
    @DefaultMessage("Filter by file or function name")
    @Key("codeSearchLabel")
    String codeSearchLabel();

    /**
     * Translated "Go to file/function".
     *
     * @return translated "Go to file/function"
     */
    @DefaultMessage("Go to file/function")
    @Key("textBoxWithCue")
    String textBoxWithCue();
}
