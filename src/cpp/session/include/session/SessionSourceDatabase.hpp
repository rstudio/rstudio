/*
 * SessionSourceDatabase.hpp
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

#ifndef SESSION_SOURCE_DATABASE_HPP
#define SESSION_SOURCE_DATABASE_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/signals.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

namespace rscore {
   class Error;
   class FilePath;
}
 
namespace session {
namespace source_database {
   
class SourceDocument : boost::noncopyable
{
public:
   SourceDocument(const std::string& type = std::string());
   virtual ~SourceDocument() {}
   // COPYING: via compiler

   // accessors
   const std::string& id() const { return id_; }
   const std::string& path() const { return path_; }
   const std::string& type() const { return type_; }
   const std::string& contents() const { return contents_; }
   const std::string& hash() const { return hash_; }
   const std::string& encoding() const { return encoding_; }
   bool dirty() const { return dirty_; }
   double created() const { return created_; }
   bool sourceOnSave() const { return sourceOnSave_; }
   int relativeOrder() const { return relativeOrder_; } 
   const rscore::json::Object& properties() const { return properties_; }
   const std::string& folds() const { return folds_; }
   std::string getProperty(const std::string& name) const;

   // is this an untitled document?
   bool isUntitled() const;

   // set contents from string
   void setContents(const std::string& contents);

   // set contents from file
   rscore::Error setPathAndContents(const std::string& path,
                                  bool allowSubstChars = true);

   rscore::Error updateDirty();

   // set dirty
   void setDirty(bool dirty)
   {
      dirty_ = dirty;
   }

   // set source on save
   void setSourceOnSave(bool sourceOnSave)
   {
      sourceOnSave_ = sourceOnSave;
   }

   void setEncoding(const std::string& encoding)
   {
      encoding_ = encoding;
   }

   void setFolds(const std::string& folds)
   {
      folds_ = folds;
   }

   void setRelativeOrder(int order) 
   {
      relativeOrder_ = order;
   }

   void checkForExternalEdit(std::time_t* pTime);

   void updateLastKnownWriteTime();

   // applies the values in the given properties object to the document's property
   // bag. this does NOT replace all of the doc's properties on the server; any
   // properties that already exist but are not present in the given object are
   // left unchanged. if an entry in the given object has a null value, that
   // property should be removed.
   void editProperties(rscore::json::Object& properties);

   void setType(const std::string& type)
   {
      type_ = type;
   }

   rscore::Error readFromJson(rscore::json::Object* pDocJson);
   void writeToJson(rscore::json::Object* pDocJson) const;

   rscore::Error writeToFile(const rscore::FilePath& filePath) const;

private:
   void editProperty(const rscore::json::Object::value_type& property);

private:
   std::string id_;
   std::string path_;
   std::string type_;
   std::string contents_;
   std::string hash_;
   std::string encoding_;
   std::string folds_;
   std::time_t lastKnownWriteTime_;
   bool dirty_;
   double created_;
   bool sourceOnSave_;
   int relativeOrder_;
   rscore::json::Object properties_;
};

bool sortByCreated(const boost::shared_ptr<SourceDocument>& pDoc1,
                   const boost::shared_ptr<SourceDocument>& pDoc2);
bool sortByRelativeOrder(const boost::shared_ptr<SourceDocument>& pDoc1,
                         const boost::shared_ptr<SourceDocument>& pDoc2);


rscore::FilePath path();
rscore::Error get(const std::string& id, boost::shared_ptr<SourceDocument> pDoc);
rscore::Error getDurableProperties(const std::string& path,
                                 rscore::json::Object* pProperties);
rscore::Error list(std::vector<boost::shared_ptr<SourceDocument> >* pDocs);
rscore::Error put(boost::shared_ptr<SourceDocument> pDoc);
rscore::Error remove(const std::string& id);
rscore::Error removeAll();

// source database events
struct Events : boost::noncopyable
{
   boost::signal<void(boost::shared_ptr<SourceDocument>)> onDocUpdated;
   boost::signal<void(const std::string&)>                onDocRemoved;
   boost::signal<void()>                                  onRemoveAll;
};

Events& events();

rscore::Error initialize();

} // namespace source_database
} // namesapce session

#endif // SESSION_SOURCE_DATABASE_HPP
