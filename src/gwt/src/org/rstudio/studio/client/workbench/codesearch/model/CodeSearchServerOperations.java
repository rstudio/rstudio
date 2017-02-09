/*
 * CodeSearchServerOperations.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
   
   /**
    * Get the definition of the specified object (if known).
    * We pass a line and pos rather than a function name because that is
    * the level of fidelity we have in the editor -- we use R on the server
    * to sort out the name of the token to lookup. Possible return types 
    * include functions defined on the search path, functions in the source
    * index, and data frames.
    */
   void getObjectDefinition(
         String line, 
         int pos,
         ServerRequestCallback<ObjectDefinition> requestCallback);

    /* 
     * Find the passed function in the search path (searching starting
     * at the environment specified by fromWhere, or the global env
     * if fromWhere is null). SearchPathFunctionDefinition will be as 
     * follows on return:
     *     - getName       -- the parsed token from line/pos or null if no
     *                        token could be parsed
     *                        
     *     - getNamespace  -- if the name was found in a namespace on the
     *                        search path then this is the namespace, else null
     *                        
     *     - getCode       -- printed variation of the function if it was
     *                        found within a namespace                   
     */
    void findFunctionInSearchPath(
         String line, 
         int pos,
         String fromWhere,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback);
    
    /*
     * Get the function identified by the following name/namespace.
     * SearchPathFunctionDefinition will be as follows on return:
     *     - getName       -- the passed name
     *                        
     *     - getNamespace  -- the passed namespace
     *                        
     *     - getCode       -- printed variation of the function (or error
     *                        message if it wasn't found      
     */
    void getSearchPathFunctionDefinition(
         String name,
         String namespace,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback);
    
    /*
     * Get a function which is known to be an S3 or S4 method. returns null
     * if no such method could be located
     */
    void getMethodDefinition(
         String name,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback);
}
