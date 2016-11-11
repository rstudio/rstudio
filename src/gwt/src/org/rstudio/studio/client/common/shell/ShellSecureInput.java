/*
 * ShellSecureInput.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.common.shell;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.crypto.PublicKeyInfo;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

/**
 * Prepare a string for secure transmission.
 * On desktop, this is a no-op.
 * For client/server, the string will be encrypted.
 */
public class ShellSecureInput
{
   public ShellSecureInput(CryptoServerOperations server)
   {
      server_ = server;
   }
  
   /**
    * Secure a string for transmission.
    *  
    * @param input   the string to secure
    * @param onReady invoked with string after it has been secured
    * @param onError invoked with an error message if operation failed
    */
   public void secureString(final String input, 
                            final CommandWithArg<String> onReady,
                            final CommandWithArg<String> onError)
   {
      if (Desktop.isDesktop())
      {
         onReady.execute(input);
      }
      else if (publicKeyInfo_ != null)
      {
         RSAEncrypt.encrypt_ServerOnly(publicKeyInfo_, input, onReady);
      }
      else
      {
         server_.getPublicKey(new ServerRequestCallback<PublicKeyInfo>() {

            @Override
            public void onResponseReceived(PublicKeyInfo publicKeyInfo)
            {
               publicKeyInfo_ = publicKeyInfo;
               RSAEncrypt.encrypt_ServerOnly(publicKeyInfo_, 
                                             input, 
                                             onReady);
            }
            
            @Override
            public void onError(ServerError error)
            {
               onError.execute(error.getUserMessage());
            }
         });
      }
   }
   
   private final CryptoServerOperations server_;
   private PublicKeyInfo publicKeyInfo_ = null;
}
