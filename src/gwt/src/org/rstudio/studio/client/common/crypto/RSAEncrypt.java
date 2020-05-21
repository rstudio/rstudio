/*
 * RSAEncrypt.java
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
package org.rstudio.studio.client.common.crypto;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class RSAEncrypt
{
   public interface ResponseCallback
   {
      void onSuccess(String encryptedData);
      void onFailure(ServerError error);
   }
   
   
   public static void encrypt_ServerOnly(
         final CryptoServerOperations server,
         final String input,
         final ResponseCallback callback)
   {
      if (Desktop.hasDesktopFrame())
      {
         // Don't encrypt for desktop, Windows can't decrypt it.
         callback.onSuccess(input);
         return;
      }
      
      if (input == null)
      {
         // fallback case for null input (see case 4375)
         callback.onSuccess("");
      }

      loader_.addCallback(new Callback()
      {
         @Override
         public void onLoaded()
         { 
            server.getPublicKey(new ServerRequestCallback<PublicKeyInfo>()
            {
               @Override
               public void onResponseReceived(PublicKeyInfo response)
               {
   
                  callback.onSuccess(encrypt(input,
                                             response.getExponent(),
                                             response.getModulo()));
               }
   
               @Override
               public void onError(ServerError error)
               {
                  callback.onFailure(error);
               }
            });
         }
      });
   }
   
   public static void encrypt_ServerOnly(final PublicKeyInfo publicKeyInfo,
                                         final String input,
                                         final CommandWithArg<String> callback)
   {
      if (Desktop.hasDesktopFrame())
      {
         // Don't encrypt for desktop, Windows can't decrypt it.
         callback.execute(input);
         return;
      }

      if (input == null)
      {
         // fallback case for null input (see case 4375)
         callback.execute("");
      }

      loader_.addCallback(new Callback()
      {
         @Override
         public void onLoaded()
         { 
            callback.execute(encrypt(input, 
                                     publicKeyInfo.getExponent(),
                                     publicKeyInfo.getModulo()));
           
         }
      });
   }
   
  
   private static native String encrypt(String value,
                                        String exponent,
                                        String modulo) /*-{
      return $wnd.encrypt(value, exponent, modulo);
   }-*/;

   private static final ExternalJavaScriptLoader loader_ =
         new ExternalJavaScriptLoader("js/encrypt.min.js");
}
