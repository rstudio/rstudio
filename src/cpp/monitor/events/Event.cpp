/*
 * Event.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <monitor/events/Event.hpp>

namespace monitor {
namespace events {

Event::Event(EventScope scope,
             int id,
             const std::string& data)
   : scope_(scope), id_(id), data_(data)
{
   username_ = core::system::username();
   pid_ = core::system::currentProcessId();
   timestamp_ =  boost::posix_time::microsec_clock::universal_time();
}

} // namespace events
} // namespace monitor

