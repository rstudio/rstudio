package org.rstudio.core.client.jsonrpc;

public abstract class RpcResponseHandler
{
   public abstract void onResponseReceived(RpcResponse response);
}
