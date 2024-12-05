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
   
  
   private static native void encrypt(String input,
                                      String key,
                                      CommandWithArg<String> callback)
   /*-{
      
      var subtle = $wnd.crypto.subtle;
      
      // The provided key is in PEM format.
      // We need to extract the inner key contents.
      var pemHeader = "-----BEGIN PUBLIC KEY-----";  // pragma: allowlist secret
      var pemFooter = "-----END PUBLIC KEY-----";    // pragma: allowlist secret
      var keyContents = key
         .replace(pemHeader, "")
         .replace(pemFooter, "")
         .replace(/\s+/g, "");
      
      // Convert that into a Uint8Array.
      var array = Uint8Array.from(
         atob(keyContents),
         function(ch) { return ch.charCodeAt(0); }
      );
      
      // Import the key.
      // https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#subjectpublickeyinfo
      var key = subtle.importKey(
         "spki",
         array.buffer,
         { name: "RSA-OAEP", hash: "SHA-256" },
         true,
         ["encrypt"]
      );
      
      key.then(function(key) {
         
         // Encrypt our message, then invoke the provided callback.
         var encoder = new TextEncoder('utf-8');
         var message = subtle.encrypt(
            { name: "RSA-OAEP" },
            key,
            encoder.encode(input)
         );
         
         message.then(function(message) {
            
            // Convert from array buffer to byte string.
            var bytes = new Uint8Array(message);
            var data = '';
            for (var i = 0, n = bytes.length; i < n; i++) {
               data += String.fromCharCode(bytes[i]);
            }
            
            // Invoke callback with that byte-string encoded as base64.
            var b64data = btoa(data);
            callback.@org.rstudio.core.client.CommandWithArg::execute(*)(b64data);
            
         });
         
      });
      
   }-*/;

}
