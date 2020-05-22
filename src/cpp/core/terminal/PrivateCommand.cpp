/*
 * PrivateCommand.cpp
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

#include <core/terminal/PrivateCommand.hpp>

#include <boost/algorithm/string/trim.hpp>

namespace rstudio {
namespace core {
namespace terminal {

namespace {

boost::posix_time::ptime now()
{
   return boost::posix_time::microsec_clock::universal_time();
}

const std::string kEol = "\r\n";

} // anonymous namespace

/*
 * Implementation note: private command output is detected by echoing start and end
 * marker (uuids) around the output, e.g. the command "ls -l" would be issued as:
 *
 * echo BOM && ls -l && EOM
 *
 * where BOM and EOM would be actual uuids. This assumes our shell supports && for
 * issuing sequential commands.
 *
 * Command is prefixed with a space, to take advantage of
 * HISTCONTROL=ignorespace (or ignoreboth) as a way to prevent command from
 * showing up in shell history.
 * 
 * In Zsh, we rely on the shell having the HIST_IGNORE_SPACE option set, which we
 * do via -g when we start Zsh. There is no attempt to detect if this setting has
 * been overridden.
 */

PrivateCommand::PrivateCommand(const std::string& command,
                               int privateCommandDelayMs,
                               int waitAfterCommandDelayMs,
                               int privateCommandTimeoutMs,
                               int postCommandTimeoutMs,
                               bool oncePerUserCommand)
   :
     command_(command),
     oncePerUserCommand_(oncePerUserCommand),
     privateCommandLoop_(false),
     lastPrivateCommand_(boost::posix_time::not_a_date_time),
     lastEnterTime_(boost::posix_time::not_a_date_time),
     outputReceivedTime_(boost::posix_time::not_a_date_time),
     pendingCommand_(false),
     privateCommandDelay_(privateCommandDelayMs),
     waitForCommandDelay_(waitAfterCommandDelayMs),
     privateCommandTimeout_(privateCommandTimeoutMs),
     postCommandTimeout_(postCommandTimeoutMs),
     firstCRLF_(std::string::npos),
     histcontrol_(std::string::npos),
     outputStart_(std::string::npos),
     outputEnd_(std::string::npos),
     timeout_(false),
     detectedWrongHistControl_(false)
{
   outputBOM_ = core::system::generateShortenedUuid();
   outputEOM_ = core::system::generateShortenedUuid();

   std::string commandBefore_ = " echo ";
   commandBefore_ += outputBOM_;
   commandBefore_ += " && echo $HISTCONTROL";
   std::string commandAfter_ = "echo ";
   commandAfter_ += outputEOM_;

   fullCommand_ = commandBefore_ + " && " + command + " && " + commandAfter_ + "\n";
}

bool PrivateCommand::onTryCapture(core::system::ProcessOperations& ops, bool hasChildProcs)
{
   boost::posix_time::ptime currentTime = now();
   if (privateCommandLoop_)
   {
      if (!output_.empty())
      {
         if (currentTime - outputReceivedTime_ > postCommandTimeout_)
         {
            resetParse();

            privateCommandLoop_ = false;
            return false; // all done
         }
         return true; // still waiting for our post-capture timeout
      }

      if (currentTime - lastPrivateCommand_ > privateCommandTimeout_)
      {
         LOG_WARNING_MESSAGE("PrivateCommand timeout");
         terminateCapture();
         ops.ptyInterrupt();
         timeout_ = true;
         return false;
      }

      return true; // still waiting for output
   }
   else
   {
      if (detectedWrongHistControl_)
         return false;

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
      timeout_ = false;

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

   // find the end of command line
   if (firstCRLF_ == std::string::npos)
   {
      firstCRLF_ = privateCommandOutput_.find(kEol, 0);
      if (firstCRLF_ == std::string::npos)
      {
         return true;
      }
      firstCRLF_ += kEol.length();
   }

   // find the echoed BOM
   if (histcontrol_ == std::string::npos)
   {
      std::string bomLine = outputBOM_ + kEol;
      histcontrol_ = privateCommandOutput_.find(bomLine, firstCRLF_);
      if (histcontrol_ == std::string::npos)
      {
         return true;
      }
      histcontrol_ += bomLine.length();
   }

   // next line has HISTCONTROL value
   if (outputStart_ == std::string::npos)
   {
      outputStart_ = privateCommandOutput_.find(kEol, histcontrol_);
      if (outputStart_ == std::string::npos)
      {
         return true;
      }

      std::string histcontrolValue = privateCommandOutput_.substr(
               histcontrol_, outputStart_ - histcontrol_);
      boost::algorithm::trim(histcontrolValue);
      outputStart_ += kEol.length();
      if (histcontrolValue != "ignorespace" && histcontrolValue != "ignoreboth")
      {
         // turn off future private commands for lifetime of this object to prevent
         // accumulation of "not-quite-private" commands in shell history

         std::string msg = "Private command disabled, unsupported $HISTCONTROL: [";
         msg += histcontrolValue;
         msg += "]";
         LOG_WARNING_MESSAGE(msg);
         detectedWrongHistControl_ = true;
      }
   }

   // find the end of output
   if (outputEnd_ == std::string::npos)
   {
      std::string eomLine = outputEOM_ + kEol;
      outputEnd_ = privateCommandOutput_.find(eomLine, outputStart_);
      if (outputEnd_ == std::string::npos)
      {
         return true;
      }
   }

   if (output_.empty())
   {
      // extract the command output
      output_ = privateCommandOutput_.substr(outputStart_, outputEnd_ - outputStart_);
      outputReceivedTime_ = now();
   }

   // Until we turn off capture, continue to ignore additional output, such as the trailing prompt
   return true;
}

std::string PrivateCommand::getPrivateOutput()
{
   if (hasCaptured()) // still in-process
      return "";

   std::string result = output_;
   output_.clear();
   return result;
}

void PrivateCommand::resetParse()
{
   outputReceivedTime_ = boost::posix_time::not_a_date_time;
   firstCRLF_ = std::string::npos;
   histcontrol_ = std::string::npos;
   outputStart_ = std::string::npos;
   outputEnd_ = std::string::npos;
   privateCommandOutput_.clear();
}

void PrivateCommand::terminateCapture()
{
   resetParse();
   privateCommandLoop_ = false;
   output_.clear();
}

bool PrivateCommand::timeout()
{
   bool didTimeout = timeout_;
   timeout_ = false;
   return didTimeout;
}


} // namespace terminal
} // namespace core
} // namespace rstudio
