/*
 * FastestSmallestTextEncoderDecoder.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
package org.rstudio.core.client.polyfill;

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.inject.Singleton;

/**
 * Provides a polyfill for window.TextEncoder() and window.TextDecoder(),
 * needed for Internet Explorer (and possibly some versions of Edge).
 * 
 * See: https://github.com/anonyco/FastestSmallestTextEncoderDecoder
 */
@Singleton
public class FastestSmallestTextEncoderDecoder
{
   public interface Resources extends ClientBundle
   {
      @Source("js/FastestSmallestTextEncoderDecoder.min.js")
      TextResource js();
   }
 
   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      DomUtils.loadScript(RES.js());
   }
}
