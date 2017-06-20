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

/*
 * The PrivateCommand class is used by terminal to silently run a command in the terminal
 * without the user seeing the input or output. Current use case is to grab the terminal's
 * environment so we can persist it. So, we don't run the private command until after the user
 * has run a command in the terminal (otherwise the terminal's environment hasn't changed). To
 * generalize this class for other private command scenarios, need to make this behavior
 * optional so the private command runs even if user hasn't entered a new command.
 */
class PrivateCommand : boost::noncopyable
{
public:
   PrivateCommand(const std::string& command,
                  int privateCommandDelayMs = 3000, // min delay between private commands
                  int waitAfterCommandDelayMs = 1500, // min delay after user command
                  int privateCommandTimeoutMs = 1000); // timeout for private command

   // Give private command opportunity to capture terminal; returns true if it does (or already did).
   bool onTryCapture(core::system::ProcessOperations& ops, bool hasChildProcs);

   bool hasCaptured() const;

   // user input, used to determine when a private command can be executed
   void userInput(const std::string& input);

   // returns true if private command parser consumed the output
   bool output(const std::string& output);

   // get output of last private command
   std::string getPrivateOutput() const;

   // force exit of private capture
   void endCapture();

   // following are aids for testing; hate to have these but alternatives were uglier
   std::string getFullCommand() const { return fullCommand_; }
   std::string getBOM() const { return outputBOM_; }
   std::string getEOM() const { return outputEOM_; }

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

   // Text output by the private command
   std::string privateCommandOutput_;

   // minimum delay between private command executions
   boost::posix_time::milliseconds privateCommandDelay_;

   // how long after a command is started do we delay before considering running a private command
   boost::posix_time::milliseconds waitForCommandDelay_;

   // how long after a private command is started do we allow for the result to be delivered?
   boost::posix_time::milliseconds privateCommandTimeout_;
};

} // namespace terminal
} // namespace core
} // namespace terminal

#endif // PRIVATE_COMMAND_HPP
