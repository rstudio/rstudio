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
 * without the user seeing the input or output. It only runs if the terminal has no child
 * processes and if the user doesn't seem to be in the middle of typing a command.
 *
 * Host must frequently call onTryCapture() to possibly fire the command, userInput() so
 * PrivateCommand can analyze if user has entered a command or is potentially in the middle of
 * typing a command, and output() to let PrivateCommand analyze output from a private command
 * it issued so it capture it and end the private-command mode.
 *
 * If oncePerUserCommand is true, the private command will only run after the user has hit
 * <enter>, so has potentially run a command. It will not run again until user has hit
 * <enter> again. Idea here is to only capture changes that can be made by a user command, such
 * as changing the environment.
 *
 * If oncePerUserCommand is false, then the private command can run as often as every
 * privateCommandTimeoutMs.
 *
 * Once PrivateCommand::hasCaptured() returns false, the output of the private command is available
 * via getPrivateOutput().
 */
class PrivateCommand : boost::noncopyable
{
public:
   PrivateCommand(
         const std::string& command,
         int privateCommandDelayMs = 3000, // min delay between private commands
         int waitAfterCommandDelayMs = 1500, // min delay after user command
         int privateCommandTimeoutMs = 1000, // timeout for private command
         bool oncePerUserCommand = true); // only run private command after user has hit <enter>

   // Give private command opportunity to capture terminal; returns true if it does (or already did).
   bool onTryCapture(core::system::ProcessOperations& ops, bool hasChildProcs);

   // If true, a private command was fired, but still receiving the output.
   bool hasCaptured() const;

   // user input, used to determine when a private command can be executed
   void userInput(const std::string& input);

   // returns true if private command parser consumed the output
   bool output(const std::string& output);

   // get output of last private command; output is reset after it is returned
   std::string getPrivateOutput();

   // force exit of private capture
   void terminateCapture();

   // following are aids for testing; hate to have these but alternatives were uglier
   std::string getFullCommand() const { return fullCommand_; }
   std::string getBOM() const { return outputBOM_; }
   std::string getEOM() const { return outputEOM_; }

private:
   void completeCapture();

private:
   std::string command_;

   // If true, only run private command after user has hit <enter> (and only once per-user <enter>)
   bool oncePerUserCommand_;

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

   // parse details
   size_t firstBOM_; // BOM in the echo'd command
   size_t firstEOM_; // EOM in the echo'd command
   size_t outputStart_; // start of output
};

} // namespace terminal
} // namespace core
} // namespace terminal

#endif // PRIVATE_COMMAND_HPP
