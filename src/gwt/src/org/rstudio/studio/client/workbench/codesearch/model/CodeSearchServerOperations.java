/*
 * CodeSearchServerOperations.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.studio.client.server.ServerRequestCallback;

public interface CodeSearchServerOperations 
{   
   /*
    * Search all currently managed code (project or open source docs
    * for a file or function matching the specified term) 
    */
   void searchCode(
         String term, 
         int maxResults,
         ServerRequestCallback<CodeSearchResults> requestCallback);
   
   /*
    * Get the definition of the specified function (if known).
    * We pass a line and pos rather than a function name because that is
    * the level of fidelity we have in the editor -- we use R on the server
    * to sort out the name of the token to lookup. Fields in the
    * FunctionDefinition will be as follows on return
    *     - getFunctionName     -- the parsed token from line/pos or
    *                              null if no token could be parsed
    *                              
    *     - getFile/getPosition -- file location if one of the files we
    *                              are managing has the definition, else null
    *                             
    *     - getSearchPathFunctionDefinition 
    *                           -- name/namespace/code if the function was
    *                              found on the search path, else null. this
    *                              is mutually exclusive with getFile
    */
   void getFunctionDefinition(
         String line, 
         int pos,
         ServerRequestCallback<FunctionDefinition> requestCallback);

    /* 
     * Find the passed function in the search path (searching starting
     * at the environment specified by fromWhere, or the global env
     * if fromWhere is null). 
     */
    void findFunctionInSearchPath(
         String name,
         String fromWhere,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback);
    
    /*
     * Get the function identified by the following name/namespace
     */
    void getSearchPathFunctionDefinition(
         String name,
         String namespace,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback);
}
