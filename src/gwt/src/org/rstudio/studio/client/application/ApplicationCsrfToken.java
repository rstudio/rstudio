/*
 * ApplicationCsrfToken.java
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

public class ApplicationCsrfToken
{
   public static String getCsrfToken()
   {
      NodeList<Element> metas = Document.get().getElementsByTagName("meta");
      for (int i = 0; i < metas.getLength(); i++)
      {
          Element meta = metas.getItem(i);
          if (StringUtil.equals(meta.getAttribute("name"), "csrf-token"))
          {
             return meta.getAttribute("content");
          }
      }
      return "";
   }
}
