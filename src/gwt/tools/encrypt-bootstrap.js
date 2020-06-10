//
// encrypt-boostrap.js
//
// Copyright (C) 2020 by RStudio, PBC
// 
//  This program is licensed to you under the terms of version 3 of the
//  GNU Affero General Public License. This program is distributed WITHOUT
//  ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
//  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
//  AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
// 

// Provides an entry point to the RSA encryption code that
// Closure Compiler won't rename

window['encrypt'] = function (value, exponent, modulo) {
   var rsa = new RSAKey();
   rsa.setPublic(modulo, exponent);
   return hex2b64(rsa.encrypt(value));
}
