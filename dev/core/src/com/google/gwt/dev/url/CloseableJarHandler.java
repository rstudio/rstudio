/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.url;

import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Opens connections to Zip files when requested and gathers them in a list for later closing.
 * <p>
 * This closing functionality makes it possible to run untrusted plugin code (for example
 * Generators) and still guarantee that there will be no accidental attempts to read from old and
 * deleted Zip files using stale InputStreams.
 */
@SuppressWarnings("restriction")
public class CloseableJarHandler extends sun.net.www.protocol.jar.Handler {

  /**
   * Passes through all URLConnection access but collects all created InputStreams.
   */
  private class CloseableUrlConnection extends URLConnection {

    private JarURLConnection jarUrlConnection;

    public CloseableUrlConnection(URL jarUrl, JarURLConnection jarUrlConnection) {
      super(jarUrl);
      this.jarUrlConnection = jarUrlConnection;
    }

    @Override
    public void addRequestProperty(String key, String value) {
      jarUrlConnection.addRequestProperty(key, value);
    }

    @Override
    public void connect() throws IOException {
      jarUrlConnection.connect();
    }

    @Override
    public boolean getAllowUserInteraction() {
      return jarUrlConnection.getAllowUserInteraction();
    }

    @Override
    public int getConnectTimeout() {
      return jarUrlConnection.getConnectTimeout();
    }

    @Override
    public Object getContent() throws IOException {
      return jarUrlConnection.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
      return jarUrlConnection.getContent(classes);
    }

    @Override
    public String getContentEncoding() {
      return jarUrlConnection.getContentEncoding();
    }

    @Override
    public int getContentLength() {
      return jarUrlConnection.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
      return jarUrlConnection.getContentLengthLong();
    }

    @Override
    public String getContentType() {
      return jarUrlConnection.getContentType();
    }

    @Override
    public long getDate() {
      return jarUrlConnection.getDate();
    }

    @Override
    public boolean getDefaultUseCaches() {
      return jarUrlConnection.getDefaultUseCaches();
    }

    @Override
    public boolean getDoInput() {
      return jarUrlConnection.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
      return jarUrlConnection.getDoOutput();
    }

    @Override
    public long getExpiration() {
      return jarUrlConnection.getExpiration();
    }

    @Override
    public String getHeaderField(int n) {
      return jarUrlConnection.getHeaderField(n);
    }

    @Override
    public String getHeaderField(String name) {
      return jarUrlConnection.getHeaderField(name);
    }

    @Override
    public long getHeaderFieldDate(String name, long defaultValue) {
      return jarUrlConnection.getHeaderFieldDate(name, defaultValue);
    }

    @Override
    public int getHeaderFieldInt(String name, int defaultValue) {
      return jarUrlConnection.getHeaderFieldInt(name, defaultValue);
    }

    @Override
    public String getHeaderFieldKey(int n) {
      return jarUrlConnection.getHeaderFieldKey(n);
    }

    @Override
    public long getHeaderFieldLong(String name, long defaultValue) {
      return jarUrlConnection.getHeaderFieldLong(name, defaultValue);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
      return jarUrlConnection.getHeaderFields();
    }

    @Override
    public long getIfModifiedSince() {
      return jarUrlConnection.getIfModifiedSince();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      InputStream inputStream = jarUrlConnection.getInputStream();
      URL jarFileURL = jarUrlConnection.getJarFileURL();
      inputStreamsByJarFilePath.put(jarFileURL.getFile(), inputStream);
      return inputStream;
    }

    @Override
    public long getLastModified() {
      return jarUrlConnection.getLastModified();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return jarUrlConnection.getOutputStream();
    }

    @Override
    public Permission getPermission() throws IOException {
      return jarUrlConnection.getPermission();
    }

    @Override
    public int getReadTimeout() {
      return jarUrlConnection.getReadTimeout();
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
      return jarUrlConnection.getRequestProperties();
    }

    @Override
    public String getRequestProperty(String key) {
      return jarUrlConnection.getRequestProperty(key);
    }

    @Override
    public URL getURL() {
      return jarUrlConnection.getURL();
    }

    @Override
    public boolean getUseCaches() {
      return jarUrlConnection.getUseCaches();
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
      jarUrlConnection.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public void setConnectTimeout(int timeout) {
      jarUrlConnection.setConnectTimeout(timeout);
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
      jarUrlConnection.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setDoInput(boolean doinput) {
      jarUrlConnection.setDoInput(doinput);
    }

    @Override
    public void setDoOutput(boolean dooutput) {
      jarUrlConnection.setDoOutput(dooutput);
    }

    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
      jarUrlConnection.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public void setReadTimeout(int timeout) {
      jarUrlConnection.setReadTimeout(timeout);
    }

    @Override
    public void setRequestProperty(String key, String value) {
      jarUrlConnection.setRequestProperty(key, value);
    }

    @Override
    public void setUseCaches(boolean usecaches) {
      jarUrlConnection.setUseCaches(usecaches);
    }

    @Override
    public String toString() {
      return jarUrlConnection.toString();
    }
  }

  private Multimap<String, InputStream> inputStreamsByJarFilePath = HashMultimap.create();

  /**
   * Closes all InputStreams that were created by the URL system that point at the given Jar file.
   */
  public void closeStreams(String jarFilePath) throws IOException {
    Collection<InputStream> inputStreams = inputStreamsByJarFilePath.get(jarFilePath);
    if (inputStreams == null) {
      return;
    }

    for (InputStream inputStream : inputStreams) {
      inputStream.close();
    }
    inputStreamsByJarFilePath.removeAll(jarFilePath);
  }

  @Override
  protected URLConnection openConnection(URL jarUrl) throws IOException {
    // Let the Jar system do the heavy lifting of opening the connection.
    JarURLConnection jarUrlConnection = (JarURLConnection) super.openConnection(jarUrl);
    // Ensures that when all connections have been closed the cached Zip file references will be
    // cleared.
    jarUrlConnection.setUseCaches(false);

    // Wrap the jar url connection in a way that will collect all created InputStreams so that they
    // can be closed.
    return new CloseableUrlConnection(jarUrl, jarUrlConnection);
  }
}
