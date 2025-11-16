/*
 * RSAEncrypt.java
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
package org.rstudio.studio.client.common.crypto;

import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONValue;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
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
      if (Desktop.isDesktop())
      {
         // Don't encrypt for desktop sessions, Windows can't decrypt it.
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
                  encrypt(input, response.getExponent(), response.getModulo(), callback);
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


   private static native String encrypt(String value,
                                        String exponent,
                                        String modulo,
                                        ResponseCallback callback) /*-{
      $wnd.encrypt(value, exponent, modulo).then(
         function(data) {
            var result = data.alg ? '$' + data.alg + '$' + data.ct : data.ct;
            callback.@org.rstudio.studio.client.common.crypto.RSAEncrypt.ResponseCallback::onSuccess(Ljava/lang/String;)(result);
         },
         function(error) {
            var errorMessage = error.message || error.toString();
            var serverError = @org.rstudio.studio.client.common.crypto.RSAEncrypt::createEncryptionError(Ljava/lang/String;)(errorMessage);
            callback.@org.rstudio.studio.client.common.crypto.RSAEncrypt.ResponseCallback::onFailure(Lorg/rstudio/studio/client/server/ServerError;)(serverError);
         }
      );
   }-*/;

   private static ServerError createEncryptionError(final String message)
   {
      return new ServerError()
      {
         @Override
         public int getCode()
         {
            return ServerError.EXECUTION;
         }

         @Override
         public String getMessage()
         {
            return message;
         }

         @Override
         public String getRedirectUrl()
         {
            return null;
         }

         @Override
         public ServerErrorCause getCause()
         {
            return null;
         }

         @Override
         public String getUserMessage()
         {
            return message;
         }

         @Override
         public JSONValue getClientInfo()
         {
            return JSONNull.getInstance();
         }
      };
   }

   private static final ExternalJavaScriptLoader loader_ =
         new ExternalJavaScriptLoader("js/encrypt.min.js");
}
