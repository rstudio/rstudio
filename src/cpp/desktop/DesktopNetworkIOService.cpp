/*
 * DesktopNetworkIOService.cpp
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

#include "DesktopNetworkIOService.hpp"

#include <core/Log.hpp>
#include <core/Error.hpp>

using namespace core;

namespace desktop {

boost::asio::io_service& ioService()
{
   static boost::asio::io_service instance;
   return instance;
}

void ioServicePoll()
{
   boost::system::error_code ec;
   ioService().poll(ec);
   if (ec)
      LOG_ERROR(Error(ec, ERROR_LOCATION));

   ioService().reset();
}


} // namespace desktop
