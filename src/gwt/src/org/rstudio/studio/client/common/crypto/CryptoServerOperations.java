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
