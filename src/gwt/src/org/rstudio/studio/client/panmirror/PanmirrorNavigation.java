/*
 * PanmirrorNavigation.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror;


import jsinterop.annotations.JsType;


@JsType
public class PanmirrorNavigation
{    
   public static final String Pos = "pos";
   public static final String Id = "id";
   public static final String Href= "href";
   public static final String Heading = "heading";
   
   public PanmirrorNavigation(String type, String location)
   {
      this.type = type;
      this.location = location;
   }
   
   public String type;
   public String location;
}
