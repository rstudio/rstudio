/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.server;

import com.google.gwt.dev.util.Util;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.valuestore.shared.WriteOperation;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Class to populate the datastore with sample data in a JSON file.
 */
public class SampleDataPopulator {

  public static void main(String args[]) {
    // TODO: cleanup argument processing and error reporting.
    if (args.length < 2) {
      printHelp();
      System.exit(-1);
    }
    try {
      if (!args[0].endsWith(RequestFactory.URL)) {
        System.err.println("Please check your URL string " + args[0]
            + ", it should end with " + RequestFactory.URL + ", exiting");
        System.exit(-1);
      }
      SampleDataPopulator populator = new SampleDataPopulator(args[0], args[1]);
      populator.populate();
    } catch (Exception ex) {
      ex.printStackTrace();
      printHelp();
    }
  }

  private static void printHelp() {
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append("Requires two arguments: the URL to post the JSON data and the path to the JSON data file.");
    System.err.println(sb.toString());
  }

  private final String url;

  private final String filePathName;

  SampleDataPopulator(String url, String filePathName) {
    this.url = url;
    this.filePathName = filePathName;
  }

  public void populate() throws JSONException, HttpException, IOException {
    JSONObject jsonObject = readAsJsonObject(readFileAsString(filePathName));
    postJsonFile(jsonObject);
  }

  @SuppressWarnings("deprecation")
  private void postJsonFile(JSONObject contentData) throws HttpException,
      IOException, JSONException {
    PostMethod post = new PostMethod(url);
    JSONObject request = new JSONObject();
    request.put(RequestDataManager.OPERATION_TOKEN, RequestFactory.SYNC);
    request.put(RequestDataManager.CONTENT_TOKEN, contentData);
    post.setRequestBody(request.toString());
    HttpClient client = new HttpClient();
    int status = client.executeMethod(post);
    JSONObject response = new JSONObject(post.getResponseBodyAsString());
    JSONArray records = response.getJSONArray(WriteOperation.CREATE.name());
    if (status == HttpStatus.SC_OK) {
      System.out.println("SUCCESS: Put " + records.length()
          + " records in the datastore!");
      return;
    }
    System.err.println("POST failed: Status line " + post.getStatusLine()
        + ", please check your URL");
  }

  private JSONObject readAsJsonObject(String string) throws JSONException {
    JSONObject jsonObject = new JSONObject(string);
    return jsonObject;
  }

  private String readFileAsString(String filePathName) {
    File file = new File(filePathName);
    return Util.readFileAsString(file);
  }

}
