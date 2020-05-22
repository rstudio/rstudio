/*
 * PrivateCommand.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
 * Relies on HISTCONTROL=ignorespace to prevent the "hidden" command from being added to
 * shell history. RStudio sets this when launching terminal, but shell config can change it.
 * If the command sees that HISTCONTROL is not set to ignorespace (or ignoreboth)
 * it will stop firing any more commands for duration of this object, otherwise users will see a
 * "weird" thing in history after virtually every command they type.
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
 *
 * Capture mode stays enabled briefly after the expected output has been received so it can
 * suppress the prompt that is displayed after the hidden command is executed. Sometimes
 * this prompt arrives in the same output string as the end of the input, but sometimes it
 * doesn't and user would see a mysterious extra prompt.
 */
class PrivateCommand : boost::noncopyable
{
public:
   PrivateCommand(
         const std::string& command,
         int privateCommandDelayMs = 3000, // min delay between private commands
         int waitAfterCommandDelayMs = 2000, // min delay after user command
         int privateCommandTimeoutMs = 1200, // timeout for private command
         int postCommandTimeoutMs = 300, // how long to suppress output after private command done
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

   // did last command timeout, value reset after this call
   bool timeout();

   // following are aids for testing; hate to have these but alternatives were uglier
   std::string getFullCommand() const { return fullCommand_; }
   std::string getBOM() const { return outputBOM_; }
   std::string getEOM() const { return outputEOM_; }

private:
   void resetParse();

private:
   std::string command_;

   // If true, only run private command after user has hit <enter> (and only once per-user <enter>)
   bool oncePerUserCommand_;

   // Are we in private command mode?
   bool privateCommandLoop_;

   // When did we last perform a private command?
   boost::posix_time::ptime lastPrivateCommand_;

   // When did user last hit Enter at the end of a line of input?
   boost::posix_time::ptime lastEnterTime_;

   // When did we successfully receive the expected private output?
   boost::posix_time::ptime outputReceivedTime_;

   // Is there a partially-typed command?
   bool pendingCommand_;

   std::string outputBOM_;
   std::string outputEOM_;
   std::string commandBefore_;
   std::string commandAfter_;
   std::string fullCommand_;

   // Raw text output by the private command
   std::string privateCommandOutput_;

   // Processed text from the private command, only available once complete
   std::string output_;

   // minimum delay between private command executions
   boost::posix_time::milliseconds privateCommandDelay_;

   // how long after a command is started do we delay before considering running a private command
   boost::posix_time::milliseconds waitForCommandDelay_;

   // how long after a private command is started do we allow for the result to be delivered?
   boost::posix_time::milliseconds privateCommandTimeout_;

   // How long after expected output received do we continue to suppress showing output?
   // Keep very short but long enough for shells that send the prompt shown after the
   // private command in a separate output event, otherwise user sees duplicate prompts
   boost::posix_time::milliseconds postCommandTimeout_;

   // parse details
   size_t firstCRLF_; // end of command
   size_t histcontrol_; // value of $HISTCONTROL
   size_t outputStart_; // start of output
   size_t outputEnd_; // end of output

   // did last private command timeout?
   bool timeout_;

   // wrong HISTCONTROL, further commands disabled
   bool detectedWrongHistControl_;
};

} // namespace terminal
} // namespace core
} // namespace terminal

#endif // PRIVATE_COMMAND_HPP
