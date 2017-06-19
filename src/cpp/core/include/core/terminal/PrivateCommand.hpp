/*
 * PrivateCommand.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef PRIVATE_COMMAND_HPP
#define PRIVATE_COMMAND_HPP

#include <string>

#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/noncopyable.hpp>

#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
namespace terminal {

class PrivateCommand : boost::noncopyable
{
public:
   PrivateCommand(const std::string& command);

   // Give private command opportunity to capture terminal; returns true if it does (or already did).
   bool onTryCapture(core::system::ProcessOperations& ops, bool hasChildProcs);

   bool hasCaptured() const;

   // user input, used to determine when a private command can be executed
   void userInput(const std::string& input);

   // returns true if private command parser consumed the output
   bool output(const std::string& output);

private:
   std::string command_;

   // Are we in private command mode? Has to be thread-safe because websocket input callback
   // reads this from separate thread. This should be a C++11 std::atomic<bool>.
   bool privateCommandLoop_;

   // When did we last perform a private command?
   boost::posix_time::ptime lastPrivateCommand_;

   // When did user last hit Enter at the end of a line of input? Protect with inputQueueMutex_.
   boost::posix_time::ptime lastEnterTime_;

   // Is there a partially-typed command? Protect with inputQueueMutex_.
   bool pendingCommand_;

   std::string outputBOM_;
   std::string outputEOM_;
   std::string commandBefore_;
   std::string commandAfter_;
   std::string fullCommand_;

   std::string privateCommandOutput_;

};

} // namespace terminal
} // namespace core
} // namespace terminal

#endif // PRIVATE_COMMAND_HPP
