/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetProxy {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: NetProxy <local-port> <remote-port> [<remote-host>]");
            return;
        }

        int localPort = Integer.parseInt(args[0]);
        int remotePort = Integer.parseInt(args[1]);
        String remoteHost = args.length < 3 ? "localhost" : args[2];
        NetProxy netProxy = new NetProxy(localPort, remoteHost, remotePort, true);
        netProxy.run();
    }

    private boolean fDumpChars;
    private int fFromPort;
    private String fToName;
    private int fToPort;
    private int fBytesSent;
    private int fBytesReceived;
    private int fConnections;
    private boolean fEnd;
    private long fStartTime = System.currentTimeMillis();

    public NetProxy(int fromPort, String toName, int toPort, boolean dumpChars) {
        fFromPort = fromPort;
        fToName = toName;
        fToPort = toPort;
        fDumpChars = dumpChars;
    }
    
    public void end() {
        fEnd = true;
    }

    protected int getBytesSent() {
        return fBytesSent;
    }

    protected int getBytesReceived() {
        return fBytesReceived;
    }
    
    private synchronized void log(int bytesSent, int bytesReceived, byte[] dataBuffer) {
        System.out.print(System.currentTimeMillis() - fStartTime);
        System.out.print("\t");
        System.out.print(bytesSent);
        System.out.print("\t");
        System.out.print(bytesReceived);
        System.out.print("\t");
        System.out.print(fBytesSent + fBytesReceived);
        System.out.print("\t");
        char ch;
        int avail = (bytesSent != 0 ? bytesSent : bytesReceived);
        if (fDumpChars) {
            int limit = (avail < 1024 ? avail : 1024);
            for (int i = 0; i < limit; ++i) {
                ch = (char)dataBuffer[i];
                if (ch >= 32 && ch < 128) {
                    System.out.print(ch);
                } else if (ch == '\n') {
                    System.out.print("\\n");
                } else if (ch == '\r') {
                    System.out.print("\\r");
                } else {
                    System.out.print('.');
                }
            }
        } else {
            // Just read up to the \r\n\r\n.
            //
            try {
                String http = new String(dataBuffer, "UTF-8");
                int endOfHeaders = http.indexOf("\r\n\r\n");
                if (endOfHeaders != -1 && http.indexOf("HTTP") != -1) {
                    http = http.replaceAll("\\r", ""); 
                    http = http.replaceAll("\\n", "\n\t\t\t\t");
                    System.out.print(http.substring(0, endOfHeaders));
                } else {
                    System.out.print("(data)");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println();
    }  
    
    private synchronized void addBytesSent(int byteCount, byte[] bytes) {
        fBytesSent += byteCount;
        log(byteCount, 0, bytes);
    }

    private synchronized void addBytesReceived(int byteCount, byte[] bytes) {
        fBytesReceived += byteCount;        
        log(0, byteCount, bytes);
    }

    public void run() {
        try {
            System.out.println("Time\tBytes Sent\tBytes Received\tTotal Bytes\tHTTP Data");
            ServerSocket serverSocket = new ServerSocket(fFromPort);
            while (!fEnd) {
                try {
                    ++fConnections;

                    /*
                     * Listen for and accept the client-initiated connection.
                     * Connect to the real server.
                     * Spawn a thread to handle the data shuffling for newly-created connection.
                     */
                    Socket clientSideSocket = serverSocket.accept();
                    clientSideSocket.setTcpNoDelay(true);

                    Socket serverSideSocket = new Socket(fToName, fToPort);
                    serverSideSocket.setTcpNoDelay(true);

                    new ClientToServerProxyConnection(clientSideSocket, serverSideSocket).start();
                    new ServerToClientProxyConnection(clientSideSocket, serverSideSocket).start();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            // Failed to even be able to listen.
            e.printStackTrace();
        }
    }
    
    private abstract class ProxyConnection extends Thread {
        private Socket fFromSocket;
        private Socket fToSocket;

        public ProxyConnection(Socket fromSocket, Socket toSocket) {
            fFromSocket = fromSocket;
            fToSocket = toSocket;
        }

        public void run() {
            try {
                InputStream fromSideInput = fFromSocket.getInputStream();
                OutputStream toSideOutput = fToSocket.getOutputStream();

                /*
                 * Spin and pass data in one direction.
                 */
                int avail;
                byte[] bytes = new byte[32768];
                while (true) {
                    // Read 'from' side
                    avail = fromSideInput.read(bytes); 
                    if (avail > 0) {
                        // Forward to 'to' side 
                        toSideOutput.write(bytes, 0, avail);
                        // Accumulate bytes received
                        recordBytesTransferred(bytes, avail);
                    } else if (avail == -1) {
                        break;
                    }
                }
            } catch (Throwable e) {
            } finally {
                try {
                    fFromSocket.close();
                    fToSocket.close();
                } catch (Throwable e) {
                }
            }
        }
        
        protected abstract void recordBytesTransferred(byte[] bytes, int avail);
    }
    
    private class ClientToServerProxyConnection extends ProxyConnection {
        public ClientToServerProxyConnection(Socket clientSideSocket, Socket serverSideSocket) {
            super(clientSideSocket, serverSideSocket);
            setName(fFromPort + " => " + fToPort + " #" + fConnections);
        }
        
        protected void recordBytesTransferred(byte[] bytes, int avail) {
            addBytesSent(avail, bytes);
        }
    }

    private class ServerToClientProxyConnection extends ProxyConnection {
        public ServerToClientProxyConnection(Socket clientSideSocket, Socket serverSideSocket) {
            super(serverSideSocket, clientSideSocket);
            setName(fFromPort + " <= " + fToPort + " #" + fConnections);
        }

        protected void recordBytesTransferred(byte[] bytes, int avail) {
            addBytesReceived(avail, bytes);
        }
    }
}
