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

/**
 * Classes used in server-side implementation of remote procedure calls.
 * <p>
 * The {@link com.google.gwt.user.server.rpc.RemoteServiceServlet RemoteServiceServlet}
 * class provides the most convenient implementation
 * of server-side GWT RPC.  This class can be used in two ways:  it can be
 * subclassed by servlets that directly implement one or more service
 * interfaces, in which case incoming RPC calls will be directed to the
 * servlet subclass itself; or it can be overridden to give finer control over
 * routing RPC calls within a server framework.  (For more details on the
 * latter, see the {@link com.google.gwt.user.server.rpc.RemoteServiceServlet#processCall(String) RemoteServiceServlet.processCall(String)} method.)
 * </p>
 * 
 * <p>
 * Alternatively, GWT RPC can be integrated into an existing framework, by using 
 * the {@link com.google.gwt.user.server.rpc.RPC RPC} class to perform GWT 
 * RPC decoding, invocation, and encoding.  RemoteServiceServlet need not 
 * be subclassed at all in this case, though reading its source is advisable.
 * </p>
 * 
 * <p>
 * Note that the default RemoteServiceServlet implementation never throws
 * exceptions to the servlet container.  All exceptions that escape the the 
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServlet#processCall(String) RemoteServiceServlet.processCall(String)}
 * method will be caught, logged in the servlet context, and will cause a generic 
 * failure message to be sent to the GWT client -- with a 500 status code.  To 
 * customize this behavior, override 
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServlet#doUnexpectedFailure(java.lang.Throwable) RemoteServiceServlet.doUnexpectedFailure(java.lang.Throwable)}.
 * </p>
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.user.server.rpc;
