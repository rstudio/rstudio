/*
 * SessionServerRpc.hpp
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

#ifndef SESSION_SERVER_RPC_HPP
#define SESSION_SERVER_RPC_HPP

#define kServerRpcSocketPath "/tmp/rstudio-rserver/session-server-rpc.socket"

#define kServerRpcSecretHeader "X-RS-Session-Server-RPC-Secret"
#define kServerRpcSecretEnvVar "RS_SESSION_SERVER_RPC_SECRET"

#endif // SESSION_SERVER_RPC_HPP

