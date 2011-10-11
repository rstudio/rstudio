/*
 * CryptoServerOperations.java
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
package org.rstudio.studio.client.common.crypto;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface CryptoServerOperations
{
   public static class PublicKeyInfo extends JavaScriptObject
   {
      protected PublicKeyInfo() {}

      public native final String getExponent() /*-{
         return this.exponent;
      }-*/;

      public native final String getModulo() /*-{
         return this.modulo;
      }-*/;
   }

   void getPublicKey(ServerRequestCallback<PublicKeyInfo> requestCallback);
}
