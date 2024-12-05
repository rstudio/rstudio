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

import org.rstudio.core.client.CommandWithArg;
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

      server.getPublicKey(new ServerRequestCallback<PublicKeyInfo>()
      {
         @Override
         public void onResponseReceived(PublicKeyInfo response)
         {
            encrypt(input, response.getKey(), callback::onSuccess);
         }

         @Override
         public void onError(ServerError error)
         {
            callback.onFailure(error);
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
      
      encrypt(input, publicKeyInfo.getKey(), callback::execute);
   }
   
  
   private static native void encrypt(String value,
                                      String publicKey,
                                      CommandWithArg<String> callback)
   /*-{
      
      var subtle = $wnd.crypto.subtle;
      
      // The provided key is in PEM format; we need to extract the inner
      // key contents and provide it as an array buffer.
      var pemHeader = "-----BEGIN PRIVATE KEY-----";  // pragma: allowlist secret
      var pemFooter = "-----END PRIVATE KEY-----";    // pragma: allowlist secret
      var keyContents = publicKey
         .replace(pemHeader, "")
         .replace(pemFooter, "")
         .replace(/\s+/g, "");
      
      var array = Uint8Array.from(
         atob(pemContents),
         function(ch) { return ch.charCodeAt(0); }
      );
      
      // Finally, import the key.
      var key = subtle.importKey(
         "spki",
         array.buffer,
         { name: "RSA-OAEP", hash: "SHA-256" },
         true,
         ["encrypt"]
      );
      
      key.then(function(key) {
         
         var encoder = new TextEncoder();
         var message = subtle.encrypt(
            { name: "RSA-OAEP" },
            key,
            encoder.encode(value)
         );
         
         message.then(function(message) {
            
            var binaryString = "";
            var byteArray = new Uint8Array(message);
            for (var i = 0; i < byteArray.length; i++) {
                binaryString += String.fromCharCode(byteArray[i]);
            }
            
            var encryptedBase64 = btoa(binaryString);
            console.log(encryptedBase64);
            callback.@org.rstudio.core.client.CommandWithArg::execute(*)(message);
         });
      });
      
   }-*/;

}
