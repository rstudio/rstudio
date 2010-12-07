/*
 * RClientState.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_SESSION_CLIENT_STATE_HPP
#define R_SESSION_CLIENT_STATE_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

namespace core {
	class Error;
} 

namespace r {
namespace session {

// singleton
class ClientState ;
ClientState& clientState();

enum ClientStateCommitType
{
   ClientStateCommitAll,
   ClientStateCommitPersistentOnly,
};
   
class ClientState : boost::noncopyable
{
private:   
   ClientState();
   friend ClientState& clientState();
     
public:
   
   void clear();
   
   void putTemporary(const std::string& scope, 
                     const std::string& name,
                     const core::json::Value& value);
   
   void putTemporary(const core::json::Object& temporaryState);
   
   void putPersistent(const std::string& scope, 
                      const std::string& name,
                      const core::json::Value& value);
   
   void putPersistent(const core::json::Object& persistentState); 
                  
   core::Error commit(ClientStateCommitType commitType,
                      const core::FilePath& stateDir);
   
   core::Error restore(const core::FilePath& stateDir);
   
   void currentState(core::json::Object* pCurrentState) const;
   
private:
   core::json::Object temporaryState_ ;
   core::json::Object persistentState_ ;
};
      
} // namespace session
} // namespace r

#endif // R_SESSION_CLIENT_STATE_HPP

