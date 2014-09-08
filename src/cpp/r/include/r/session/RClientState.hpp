/*
 * RClientState.hpp
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

#ifndef R_SESSION_CLIENT_STATE_HPP
#define R_SESSION_CLIENT_STATE_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

namespace rstudiocore {
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
                     const rstudiocore::json::Value& value);
   
   void putTemporary(const rstudiocore::json::Object& temporaryState);
   
   void putPersistent(const std::string& scope, 
                      const std::string& name,
                      const rstudiocore::json::Value& value);
   
   void putPersistent(const rstudiocore::json::Object& persistentState); 

   void putProjectPersistent(const std::string& scope,
                             const std::string& name,
                             const rstudiocore::json::Value& value);

   void putProjectPersistent(const rstudiocore::json::Object& projectPersistentState);
   rstudiocore::json::Value getProjectPersistent(std::string scope,
                                          std::string name);

   rstudiocore::Error commit(ClientStateCommitType commitType,
                      const rstudiocore::FilePath& stateDir,
                      const rstudiocore::FilePath& projectStateDir);
   
   rstudiocore::Error restore(const rstudiocore::FilePath& stateDir,
                       const rstudiocore::FilePath& projectStateDir);
   
   void currentState(rstudiocore::json::Object* pCurrentState) const;
   
private:
   void restoreGlobalState(const rstudiocore::FilePath& stateFile);
   void restoreProjectState(const rstudiocore::FilePath& stateFile);

private:
   rstudiocore::json::Object temporaryState_ ;
   rstudiocore::json::Object persistentState_ ;
   rstudiocore::json::Object projectPersistentState_;
};
      
} // namespace session
} // namespace r

#endif // R_SESSION_CLIENT_STATE_HPP

