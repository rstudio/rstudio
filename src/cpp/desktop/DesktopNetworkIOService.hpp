/*
 * DesktopNetworkIOService.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOP_NETWORK_IO_SERVICE_HPP
#define DESKTOP_NETWORK_IO_SERVICE_HPP

#include <boost/asio/io_service.hpp>

namespace desktop {

boost::asio::io_service& ioService();

void ioServicePoll();

} // namespace desktop

#endif // DESKTOP_NETWORK_IO_SERVICE_HPP
