/*
 * PrivateCommand.cpp
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

#include <core/terminal/PrivateCommand.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace terminal {

// minimum delay between private command executions
const boost::posix_time::milliseconds kPrivateCommandDelay = boost::posix_time::milliseconds(3000);

// how long after a command is started do we delay before considering running a private command
const boost::posix_time::milliseconds kWaitForCommandDelay = boost::posix_time::milliseconds(1500);

// how long after a private command is started do we allow for the result to be delivered?
const boost::posix_time::milliseconds kPrivateCommandMaxDuration = boost::posix_time::milliseconds(1000);

namespace {

boost::posix_time::ptime now()
{
   return boost::posix_time::microsec_clock::universal_time();
}

} // anonymous namespace

PrivateCommand::PrivateCommand(const std::string& command)
   :
     command_(command),
     privateCommandLoop_(false),
     lastPrivateCommand_(boost::posix_time::not_a_date_time),
     lastEnterTime_(boost::posix_time::not_a_date_time),
     pendingCommand_(true)

{
   outputBOM_ = core::system::generateUuid(false);
   outputEOM_ = core::system::generateUuid(true);

   std::string commandBefore_ = "echo ";
   commandBefore_ += outputBOM_;
   std::string commandAfter_ = "echo ";
   commandAfter_ += outputEOM_;

   fullCommand_ = commandBefore_ + " && " + command + " && " + commandAfter_ + "\n";
}

bool PrivateCommand::onTryCapture(core::system::ProcessOperations& ops, bool hasChildProcs)
{
   boost::posix_time::ptime currentTime = now();
   if (privateCommandLoop_)
   {
      // already running private command

      // TODO (gary)
      // safeguard timeout here to exit private command loop if parsing fails after
      // a couple of seconds!

      return true;
   }
   else
   {
      if (hasChildProcs)
         return false;

      if (pendingCommand_ || lastEnterTime_.is_not_a_date_time())
      {
         // We don't start a private command if something is being typed, or a command has never
         // been run. Environment hasn't changed in either of those cases.
         return false;
      }

      if (currentTime - kWaitForCommandDelay <= lastEnterTime_)
      {
         // not enough time has elapsed since last command was submitted
         return false;
      }

      if (!lastPrivateCommand_.is_not_a_date_time() &&
          currentTime - kPrivateCommandDelay <= lastPrivateCommand_)
      {
         // not enough time has elapsed since last private command ran
         return false;
      }

      if (!lastPrivateCommand_.is_not_a_date_time() &&
          lastPrivateCommand_ > lastEnterTime_)
      {
         // Hasn't been a new command executed since our last private command, no need
         // to run it.
         return false;
      }

      lastPrivateCommand_ = currentTime;
      privateCommandOutput_.clear();
      privateCommandLoop_ = true;

      // send the command
      Error error = ops.writeToStdin(fullCommand_, false);
      if (error)
      {
         LOG_ERROR(error);
         privateCommandLoop_ = false;
         lastPrivateCommand_ = boost::posix_time::pos_infin; // disable private commands
         return false;
      }
      return true;
   }
   return false;
}

bool PrivateCommand::hasCaptured() const
{
   return privateCommandLoop_;
}

void PrivateCommand::userInput(const std::string& input)
{
   if (!input.empty() && (*input.rbegin() == '\r' || *input.rbegin() == '\n'))
   {
      lastEnterTime_ = now();
      pendingCommand_ = false;
   }
   else
   {
      pendingCommand_ = true;
   }
}

bool PrivateCommand::output(const std::string& output)
{
   if (!hasCaptured())
   {
      return false;
   }

   privateCommandOutput_.append(output);
   return true;
}

} // namespace terminal
} // namespace core
} // namespace rstudio
