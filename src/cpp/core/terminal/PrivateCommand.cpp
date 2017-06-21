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

namespace {

boost::posix_time::ptime now()
{
   return boost::posix_time::microsec_clock::universal_time();
}

} // anonymous namespace

/*
 * Implementation note: private command output is detected by echoing start and end
 * marker (uuids) around the output, e.g. the command "ls -l" would be issued as:
 *
 * echo BOM && ls -l && EOM
 *
 * where BOM and EOM would be actual uuids. This assumes our shell supports && for
 * issuing sequential commands.
 */

PrivateCommand::PrivateCommand(const std::string& command,
                               int privateCommandDelayMs,
                               int waitAfterCommandDelayMs,
                               int privateCommandTimeoutMs,
                               bool oncePerUserCommand)
   :
     command_(command),
     oncePerUserCommand_(oncePerUserCommand),
     privateCommandLoop_(false),
     lastPrivateCommand_(boost::posix_time::not_a_date_time),
     lastEnterTime_(boost::posix_time::not_a_date_time),
     pendingCommand_(false),
     privateCommandDelay_(privateCommandDelayMs),
     waitForCommandDelay_(waitAfterCommandDelayMs),
     privateCommandTimeout_(privateCommandTimeoutMs),
     firstBOM_(std::string::npos),
     firstEOM_(std::string::npos),
     outputStart_(std::string::npos)
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
      // try to prevent private command from getting stuck
      if (currentTime - lastPrivateCommand_ > privateCommandTimeout_)
      {
         terminateCapture();
         ops.ptyInterrupt();
         return false;
      }

      return true;
   }
   else
   {
      if (hasChildProcs)
         return false;

      if (pendingCommand_ || (oncePerUserCommand_ && lastEnterTime_.is_not_a_date_time()))
      {
         // We don't start a private command if something is being typed, or a command has never
         // been run.
         lastPrivateCommand_ = currentTime;
         return false;
      }

      if (!lastEnterTime_.is_not_a_date_time() &&
          (currentTime - waitForCommandDelay_ <= lastEnterTime_))
      {
         // not enough time has elapsed since last command was submitted
         return false;
      }

      if (!lastPrivateCommand_.is_not_a_date_time() &&
          currentTime - privateCommandDelay_ <= lastPrivateCommand_)
      {
         // not enough time has elapsed since last private command ran
         return false;
      }

      if (oncePerUserCommand_ &&
          !lastPrivateCommand_.is_not_a_date_time() &&
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

   // find the first BOM (from the command echo)
   if (firstBOM_ == std::string::npos)
   {
      firstBOM_ = privateCommandOutput_.find(outputBOM_, 0);
      if (firstBOM_ == std::string::npos)
      {
         return true;
      }
      firstBOM_ += outputBOM_.length();
   }

   // find the first EOM (from the command echo)
   if (firstEOM_ == std::string::npos)
   {
      firstEOM_ = privateCommandOutput_.find(outputEOM_, firstBOM_);
      if (firstEOM_ == std::string::npos)
      {
         return true;
      }
      firstEOM_ = firstEOM_ + outputEOM_.length();
   }

   // find the start of output
   if (outputStart_ == std::string::npos)
   {
      std::string bomLine = outputBOM_ + "\n";
      outputStart_ = privateCommandOutput_.find(bomLine, firstEOM_);
      if (outputStart_ == std::string::npos)
      {
         return true;
      }
      outputStart_ += bomLine.length();
   }

   // find the end of output
   std::string eomLine = outputEOM_ + "\n";
   size_t outputEnd = privateCommandOutput_.find(eomLine, outputStart_);
   if (outputEnd == std::string::npos)
   {
      return true;
   }

   // extract the command output
   privateCommandOutput_ = privateCommandOutput_.substr(outputStart_, outputEnd - outputStart_);

   completeCapture();
   return false;
}

std::string PrivateCommand::getPrivateOutput()
{
   if (hasCaptured()) // still in-process
      return "";

   std::string result = privateCommandOutput_;
   privateCommandOutput_.clear();
   return result;
}

void PrivateCommand::completeCapture()
{
   privateCommandLoop_ = false;
   firstBOM_ = std::string::npos;
   firstEOM_ = std::string::npos;
   outputStart_ = std::string::npos;
}

void PrivateCommand::terminateCapture()
{
   completeCapture();
   privateCommandOutput_.clear();
}

} // namespace terminal
} // namespace core
} // namespace rstudio
