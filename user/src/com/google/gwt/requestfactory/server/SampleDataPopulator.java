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

import com.google.gwt.requestfactory.client.DefaultRequestTransport;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
      if (!args[0].endsWith(DefaultRequestTransport.URL)) {
        System.err.println("Please check your URL string " + args[0]
            + ", it should end with " + DefaultRequestTransport.URL + ", exiting");
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

  public void populate() throws JSONException, IOException {
    JSONObject jsonObject = readAsJsonObject(readFileAsString(filePathName));
    postJsonFile(jsonObject);
  }

  @SuppressWarnings("deprecation")
  private void postJsonFile(JSONObject contentData) throws IOException,
      JSONException {
    HttpPost post = new HttpPost(url);
    JSONObject request = new JSONObject();
    request.put(RequestData.OPERATION_TOKEN, "DOESNT_WORK");
    request.put(RequestData.CONTENT_TOKEN, contentData);
    StringEntity reqEntity = new StringEntity(request.toString());
    post.setEntity(reqEntity);
    HttpClient client = new DefaultHttpClient();
    HttpResponse response = client.execute(post);
    HttpEntity resEntity = response.getEntity();
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      System.out.println("SUCCESS: Put " + resEntity.getContentLength()
          + " records in the datastore!");
      return;
    }
    System.err.println("POST failed: Status line " + response.getStatusLine()
        + ", please check your URL");
  }

  private JSONObject readAsJsonObject(String string) throws JSONException {
    JSONObject jsonObject = new JSONObject(string);
    return jsonObject;
  }

  // ugly method, refactor later when cleaning up this class.
  private byte[] readFileAsBytes(String filePathName) {
    File file = new File(filePathName);
    FileInputStream fileInputStream = null;
    byte bytes[] = null;
    try {
      fileInputStream = new FileInputStream(file);
      int byteLength = (int) file.length();
      bytes = new byte[byteLength];
      int byteOffset = 0;
      while (byteOffset < byteLength) {
        int bytesReadCount = fileInputStream.read(bytes, byteOffset, byteLength
            - byteOffset);
        if (bytesReadCount == -1) {
          return null;
        }
        byteOffset += bytesReadCount;
      }
    } catch (IOException e) {
      // Ignored.
    } finally {
      try {
        if (fileInputStream != null) {
          fileInputStream.close();
        }
      } catch (IOException e) {
        // ignored
      }
    }
    return bytes;
  }
  
  private String readFileAsString(String filePathName) {
    byte bytes[] = readFileAsBytes(filePathName);
    if (bytes != null) {
      try {
        return new String(bytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // Ignored.
      }
      return null;
    }
    return null;
  }

}
