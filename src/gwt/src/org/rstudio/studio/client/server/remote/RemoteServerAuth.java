/*
 * RemoteServerAuth.java
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
package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.RootPanel;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.jsonrpc.RequestLog;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
import org.rstudio.core.client.jsonrpc.RequestLogEntry.ResponseType;
import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.core.client.jsonrpc.RpcResponse;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import java.util.ArrayList;

class RemoteServerAuth
{
   public static final int CREDENTIALS_UPDATE_SUCCESS = 1;
   public static final int CREDENTIALS_UPDATE_FAILURE = 2;
   public static final int CREDENTIALS_UPDATE_UNSUPPORTED = 3;

   public RemoteServerAuth(RemoteServer remoteServer)
   {
      remoteServer_ = remoteServer;
   }

   private Timer periodicUpdateTimer_ = null;

   public void schedulePeriodicCredentialsUpdate()
   {
      // create the callback
      periodicUpdateTimer_ = new Timer() {
         @Override
         public void run()
         {
            updateCredentials(new ServerRequestCallback<Integer>() {

               @Override
               public void onResponseReceived(Integer response)
               {
                  switch(response)
                  {
                  case CREDENTIALS_UPDATE_SUCCESS:
                     // do nothing (we just successfully updated our
                     // credentials)
                     break;

                  case CREDENTIALS_UPDATE_FAILURE:
                     // we are not authorized, blow the client away
                     remoteServer_.handleUnauthorizedError();
                     break;

                  case CREDENTIALS_UPDATE_UNSUPPORTED:
                     // not supported by the back end so cancel the timer
                     periodicUpdateTimer_.cancel();
                     break;
                  }
               }

               @Override
               public void onError(ServerError serverError)
               {
                  // if method is not supported then cancel the timer
                  Debug.logError(serverError);
               }
            });


         }
      };

      // schedule for every 5 minutes
      final int kMinutes = 5;
      int milliseconds = kMinutes * 60 * 1000;
      periodicUpdateTimer_.scheduleRepeating(milliseconds);
   }

   public void attemptToUpdateCredentials()
   {
      updateCredentials(new ServerRequestCallback<Integer>() {

         @Override
         public void onResponseReceived(Integer response)
         {
            // this method does nothing in the case of both successfully
            // updating credentials and method not found. however, if
            // the credentials update fails then it needs to blow
            // away the client

            if (response.intValue() == CREDENTIALS_UPDATE_FAILURE)
            {
               remoteServer_.handleUnauthorizedError();
            }
         }

         @Override
         public void onError(ServerError serverError)
         {
            Debug.logError(serverError);
         }
      });
   }

   // save previous form as a precaution against forms which are not
   // cleaned up due to the submit handler not being called
   private static ArrayList<FormPanel> previousUpdateCredentialsForms_ =
                                            new ArrayList<FormPanel>();

   private void safeCleanupPreviousUpdateCredentials()
   {
      try
      {
         for (int i=0; i<previousUpdateCredentialsForms_.size(); i++)
         {
            FormPanel formPanel = previousUpdateCredentialsForms_.get(i);
            RootPanel.get().remove(formPanel);
         }

         previousUpdateCredentialsForms_.clear();
      }
      catch(Throwable e)
      {
      }
   }

   public void updateCredentials(
                         final ServerRequestCallback<Integer> requestCallback)
   {
      // safely cleanup any previously active update credentials forms
      safeCleanupPreviousUpdateCredentials();

      // create a hidden form panel to submit the update credentials to
      // (we do this so GWT manages the trickiness associated with
      // managing and reading the contents of a hidden iframe)
      final FormPanel updateCredentialsForm = new FormPanel();
      updateCredentialsForm.setMethod(FormPanel.METHOD_GET);
      updateCredentialsForm.setEncoding(FormPanel.ENCODING_URLENCODED);

      // form url
      String url = remoteServer_.getApplicationURL("auth-update-credentials");
      updateCredentialsForm.setAction(url);

      // request log entry (fake up a json rpc method call to conform
      // to the data format expected by RequestLog
      String requestId = Integer.toString(Random.nextInt());
      String requestData = createRequestData();
      final RequestLogEntry logEntry = RequestLog.log(requestId, requestData);

      // form submit complete handler
      updateCredentialsForm.addSubmitCompleteHandler(new SubmitCompleteHandler(){

         public void onSubmitComplete(SubmitCompleteEvent event)
         {
            // parse the results
            String results = event.getResults();
            RpcResponse response = RpcResponse.parse(event.getResults());
            if (response != null)
            {
               logEntry.logResponse(ResponseType.Normal, results);

               // check for error
               RpcError rpcError = response.getError();
               if (rpcError != null)
               {
                  if (rpcError.getCode() == RpcError.METHOD_NOT_FOUND)
                  {
                     requestCallback.onResponseReceived(CREDENTIALS_UPDATE_UNSUPPORTED);
                  }
                  else
                  {
                     requestCallback.onError(new RemoteServerError(rpcError));
                  }
               }
               else // must be a valid response
               {
                  Bool authenticated = response.getResult();
                  if (authenticated.getValue())
                  {
                     requestCallback.onResponseReceived(CREDENTIALS_UPDATE_SUCCESS);
                  }
                  else
                  {
                     requestCallback.onResponseReceived(CREDENTIALS_UPDATE_FAILURE);
                  }
               }
            }
            else // error parsing results
            {
               logEntry.logResponse(ResponseType.Error, results);

               // form message
               String msg = "Error parsing results: " +
                            (results != null ? results : "(null)");

               // we don't expect this so debug log to flag our attention
               Debug.log("UPDATE CREDENTIALS: " + msg);

               // return the error
               RpcError rpcError = RpcError.create(RpcError.PARSE_ERROR, msg);
               requestCallback.onError(new RemoteServerError(rpcError));
            }

            // remove the hidden form (from both last-ditch list and DOM)
            previousUpdateCredentialsForms_.remove(updateCredentialsForm);
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
               public void execute()
               {
                  RootPanel.get().remove(updateCredentialsForm);
               }
            });
         }
      });

      // add the (hidden) form panel to the document and last ditch list
      RootPanel.get().add(updateCredentialsForm, -1000, -1000);
      previousUpdateCredentialsForms_.add(updateCredentialsForm);

      // submit the form
      updateCredentialsForm.submit();
   }

   private String createRequestData()
   {
      JSONObject request = new JSONObject();
      request.put("method", new JSONString("update_credentials"));
      request.put("params", new JSONArray());
      return request.toString();
   }

   private final RemoteServer remoteServer_;
}
