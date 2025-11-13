//
// encrypt-boostrap.js
//
// Copyright (C) 2022 by Posit Software, PBC
//
//  This program is licensed to you under the terms of version 3 of the
//  GNU Affero General Public License. This program is distributed WITHOUT
//  ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
//  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
//  AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
//

function rsaJsEncryptAsync(value, exponent, modulo) {
   var rsa = new RSAKey();
   rsa.setPublic(modulo, exponent);
   return Promise.resolve({ 'ct': hex2b64(rsa.encrypt(value)) });
}

function hex2b64url(str) {
   return hex2b64(str).replace(/=/g, '').replace(/[+]/g, '-').replace(/[/]/g, '_');
}

// Provides an entry point to the RSA encryption code that
// Closure Compiler won't rename

window['encrypt'] = function (value, exponent, modulo) {
   if (window.isSecureContext && window.crypto && window.crypto.subtle) {
      var n = /** @type {!string} */(hex2b64(modulo));
      return window.crypto.subtle.importKey(
         'jwk',
         {
            kty: 'RSA',
            alg: 'RSA-OAEP-256',
            n: hex2b64url(modulo),
            e: hex2b64url(exponent),
            ext: false,
         },
         {
            name: 'RSA-OAEP',
            hash: 'SHA-256',
         },
         false,
         ['encrypt'],
      ).then(function(key) {
         return crypto.subtle.encrypt(
            { name: 'RSA-OAEP' },
            key,
            new TextEncoder().encode(value),
         );
      }).then(function(buffer) {
         var data = new Uint8Array(buffer);
         var result = '';
         for (var i = 0; i < data.length; i++) {
            result += String.fromCharCode(data[i]);
         }
         return { 'ct': window.btoa(result), 'alg': 'RSA-OAEP' };
      }).catch(function(err) {
         console.error(err);
         return rsaJsEncryptAsync(value, exponent, modulo);
      });
   } else {
      return rsaJsEncryptAsync(value, exponent, modulo);
   }
}
