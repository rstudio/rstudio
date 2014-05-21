/*
 * PackratStatus.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.packrat.model;

import com.google.gwt.core.client.JavaScriptObject;

// This class represents the JSON value the server returns from
// get_packrat_status.
public class PackratStatus extends JavaScriptObject
{
   protected PackratStatus()
   {
   }

   // One JSNI native method per field in the data frame returned by 
   // packrat::status. 
}
