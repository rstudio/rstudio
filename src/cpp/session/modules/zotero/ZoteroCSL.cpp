/*
 * ZoteroCSL.cpp
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

#include "ZoteroCSL.hpp"

#include <core/Algorithm.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <shared_core/SafeConvert.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

namespace {


// Provides a map that contains zotero field names and their corresponding
// csl fields names for a given zotero type.
// The field mappings were generated from the explanation provided by Zotero at:
// https://aurimasv.github.io/z2csl/typeMap.xml#map-artwork
std::map<std::string, std::string> cslFieldNames(std::string zoteroType)
{
  std::map<std::string, std::string> transforms;

   // Per zotero type transformations
   if (zoteroType == "artwork")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["artworkMedium"] = "medium";
      transforms["artworkSize"] = "dimensions";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["artist"] = "author";
      // contributor not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "audioRecording")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["audioRecordingFormat"] = "medium";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["composer"] = "composer";
      // contributor not representable in Zotero
      transforms["performer"] = "author";
      // wordsBy not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["label"] = "publisher";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["place"] = "publisher-place";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "bill")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["billNumber"] = "number";
      transforms["code"] = "container-title";
      transforms["codePages"] = "page";
      transforms["codeVolume"] = "volume";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      // cosponsor not representable in Zotero
      transforms["sponsor"] = "author";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["history"] = "references";
      transforms["language"] = "language";
      transforms["legislativeBody"] = "authority";
      // rights not representable in Zotero
      transforms["section"] = "section";
      transforms["session"] = "chapter-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }

   else if (zoteroType == "blogPost")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["blogTitle"] = "container-title";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // commenter not representable in Zotero
      // contributor not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["websiteType"] = "genre";
   }
   else if (zoteroType == "book")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["edition"] = "edition";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["numPages"] = "number-of-pages";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      transforms["seriesNumber"] = "collection-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "bookSection")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["bookTitle"] = "container-title";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      transforms["bookAuthor"] = "container-author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["edition"] = "edition";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      transforms["seriesNumber"] = "collection-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "case")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["caseName"] = "title";
      transforms["court"] = "authority";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      // counsel not representable in Zotero
      transforms["dateDecided"] = "issued";
      transforms["docketNumber"] = "number";
      transforms["extra"] = "note";
      transforms["firstPage"] = "page";
      transforms["history"] = "references";
      transforms["language"] = "language";
      transforms["reporter"] = "container-title";
      transforms["reporterVolume"] = "volume";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "computerProgram")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      transforms["company"] = "publisher";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      transforms["programmer"] = "author";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["libraryCatalog"] = "source";
      transforms["place"] = "publisher-place";
      transforms["programmingLanguage"] = "genre";
      // rights not representable in Zotero
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["system"] = "medium";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["version"] = "version";
   }
   else if (zoteroType == "conferencePaper")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      transforms["conferenceName"] = "event";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["DOI"] = "DOI";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["proceedingsTitle"] = "container-title";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "dictionaryEntry")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["dictionaryTitle"] = "container-title";
      transforms["edition"] = "edition";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      transforms["seriesNumber"] = "collection-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "document")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["reviewedAuthor"] = "reviewed-author";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "email")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["recipient"] = "recipient";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["subject"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "encyclopediaArticle")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["edition"] = "edition";
      transforms["encyclopediaTitle"] = "container-title";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      transforms["seriesNumber"] = "collection-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "film")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      transforms["director"] = "author";
      // producer not representable in Zotero
      // scriptwriter not representable in Zotero
      transforms["date"] = "issued";
      transforms["distributor"] = "publisher";
      transforms["extra"] = "note";
      transforms["genre"] = "genre";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["videoRecordingFormat"] = "medium";
   }
   else if (zoteroType == "forumPost")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["forumTitle"] = "container-title";
      transforms["language"] = "language";
      transforms["postType"] = "genre";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "hearing")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["committee"] = "section";
      // creator not representable in Zotero
      transforms["contributor"] = "author";
      transforms["date"] = "issued";
      transforms["documentNumber"] = "number";
      transforms["extra"] = "note";
      transforms["history"] = "references";
      transforms["language"] = "language";
      transforms["legislativeBody"] = "authority";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["session"] = "chapter-number";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "instantMessage")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["recipient"] = "recipient";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "interview")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      transforms["interviewee"] = "author";
      transforms["interviewer"] = "interviewer";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["interviewMedium"] = "medium";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "journalArticle")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["editor"] = "editor";
      transforms["reviewedAuthor"] = "reviewed-author";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["DOI"] = "DOI";
      transforms["extra"] = "note";
      transforms["ISSN"] = "ISSN";
      transforms["issue"] = "issue";
      transforms["journalAbbreviation"] = "container-title-short";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["pages"] = "page";
      transforms["publicationTitle"] = "container-title";
      // rights not representable in Zotero
      transforms["series"] = "collection-title";
      // seriesText not representable in Zotero
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "letter")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["recipient"] = "recipient";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["letterType"] = "genre";
      transforms["libraryCatalog"] = "source";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "magazineArticle")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["reviewedAuthor"] = "reviewed-author";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["ISSN"] = "ISSN";
      transforms["issue"] = "issue";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["pages"] = "page";
      transforms["publicationTitle"] = "container-title";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "manuscript")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["manuscriptType"] = "genre";
      transforms["numPages"] = "number-of-pages";
      transforms["place"] = "publisher-place";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "map")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["cartographer"] = "author";
      // contributor not representable in Zotero
      transforms["seriesEditor"] = "collection-editor";
      transforms["date"] = "issued";
      transforms["edition"] = "edition";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["mapType"] = "genre";
      transforms["place"] = "publisher-place";
      transforms["publisher"] = "publisher";
      // rights not representable in Zotero
      transforms["scale"] = "scale";
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "newspaperArticle")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["reviewedAuthor"] = "reviewed-author";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["edition"] = "edition";
      transforms["extra"] = "note";
      transforms["ISSN"] = "ISSN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["publicationTitle"] = "container-title";
      // rights not representable in Zotero
      transforms["section"] = "section";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "patent")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["applicationNumber"] = "call-number";
      // assignee not representable in Zotero
      // country not representable in Zotero
      // creator not representable in Zotero
      // attorneyAgent not representable in Zotero
      // contributor not representable in Zotero
      transforms["inventor"] = "author";
      transforms["extra"] = "note";
      transforms["filingDate"] = "submitted";
      transforms["issueDate"] = "issued";
      transforms["issuingAuthority"] = "authority";
      transforms["language"] = "language";
      transforms["legalStatus"] = "status";
      transforms["pages"] = "page";
      transforms["patentNumber"] = "number";
      transforms["place"] = "publisher-place";
      transforms["priorityNumbers"] = "issue";
      transforms["references"] = "references";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "podcast")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["audioFileType"] = "medium";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      // guest not representable in Zotero
      transforms["podcaster"] = "author";
      transforms["episodeNumber"] = "number";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "presentation")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      // creator not representable in Zotero
      // contributor not representable in Zotero
      transforms["presenter"] = "author";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["meetingName"] = "event";
      transforms["place"] = "publisher-place";
      transforms["presentationType"] = "genre";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "radioBroadcast")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["audioRecordingFormat"] = "medium";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      // castMember not representable in Zotero
      // contributor not representable in Zotero
      transforms["director"] = "author";
      // guest not representable in Zotero
      // producer not representable in Zotero
      // scriptwriter not representable in Zotero
      transforms["date"] = "issued";
      transforms["episodeNumber"] = "number";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["network"] = "publisher";
      transforms["place"] = "publisher-place";
      transforms["programTitle"] = "container-title";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "report")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["seriesEditor"] = "collection-editor";
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["institution"] = "publisher";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["pages"] = "page";
      transforms["place"] = "publisher-place";
      transforms["reportNumber"] = "number";
      transforms["reportType"] = "genre";
      // rights not representable in Zotero
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "statute")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["code"] = "container-title";
      transforms["codeNumber"] = "volume";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["dateEnacted"] = "issued";
      transforms["extra"] = "note";
      transforms["history"] = "references";
      transforms["language"] = "language";
      transforms["nameOfAct"] = "title";
      transforms["pages"] = "page";
      transforms["publicLawNumber"] = "number";
      // rights not representable in Zotero
      transforms["section"] = "section";
      transforms["session"] = "chapter-number";
      transforms["shortTitle"] = "title-short";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "thesis")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numPages"] = "number-of-pages";
      transforms["place"] = "publisher-place";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["thesisType"] = "genre";
      transforms["title"] = "title";
      transforms["university"] = "publisher";
      transforms["url"] = "URL";
   }
   else if (zoteroType == "tvBroadcast")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      // castMember not representable in Zotero
      // contributor not representable in Zotero
      transforms["director"] = "author";
      // guest not representable in Zotero
      // producer not representable in Zotero
      // scriptwriter not representable in Zotero
      transforms["date"] = "issued";
      transforms["episodeNumber"] = "number";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["network"] = "publisher";
      transforms["place"] = "publisher-place";
      transforms["programTitle"] = "container-title";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["videoRecordingFormat"] = "medium";
   }
   else if (zoteroType == "videoRecording")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      transforms["archive"] = "archive";
      transforms["archiveLocation"] = "archive_location";
      transforms["callNumber"] = "call-number";
      // creator not representable in Zotero
      // castMember not representable in Zotero
      // contributor not representable in Zotero
      transforms["director"] = "author";
      // producer not representable in Zotero
      // scriptwriter not representable in Zotero
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["ISBN"] = "ISBN";
      transforms["language"] = "language";
      transforms["libraryCatalog"] = "source";
      transforms["numberOfVolumes"] = "number-of-volumes";
      transforms["place"] = "publisher-place";
      // rights not representable in Zotero
      transforms["runningTime"] = "dimensions";
      transforms["seriesTitle"] = "collection-title";
      transforms["shortTitle"] = "title-short";
      transforms["studio"] = "publisher";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["videoRecordingFormat"] = "medium";
      transforms["volume"] = "volume";
   }
   else if (zoteroType == "webpage")
   {
      transforms["abstractNote"] = "abstract";
      transforms["accessDate"] = "accessed";
      // creator not representable in Zotero
      transforms["author"] = "author";
      // contributor not representable in Zotero
      transforms["translator"] = "translator";
      transforms["date"] = "issued";
      transforms["extra"] = "note";
      transforms["language"] = "language";
      // rights not representable in Zotero
      transforms["shortTitle"] = "title-short";
      transforms["title"] = "title";
      transforms["url"] = "URL";
      transforms["websiteTitle"] = "container-title";
      transforms["websiteType"] = "genre";
   }
   return transforms;
}

// The following are the fields that should be represented
// as dates in CSL.
//
// These fields don't have a Zotero representation, but are included
// here for completeness
//    container
//    original-date
//    event-date
bool isDateValue(std::string cslFieldName)
{
   std::vector<std::string> dateFields = {
                                          "accessed",
                                          "container",
                                          "event-date",
                                          "issued",
                                          "original-date",
                                          "submitted"
   };
   return std::find(dateFields.begin(),
                    dateFields.end(),
                    cslFieldName) != dateFields.end();
}

// Transforms the value, if needed. Currently the only transform
// that occurs will be to write a date value as a complex CSL Date.
// All other values just pass through as is.
json::Value transformValue(std::string cslFieldName, std::string value)
{
   if (isDateValue(cslFieldName)) {

      // The form of the string is 0-3 numbers that represent
      // year-month-day
      // followed by a space and then an optional raw value like
      // yyyy/mm/dd

      // Split the date string at the space. The left half is the formatted date
      // the right half is the raw date string. The left half will always be there
      // the right 'raw' form is optional
      json::Object dateJson;
      std::string date_parts = value.substr(0, value.find(' '));
      std::string date_raw;

      // If the left 'date_parts' doesn't represent the whole string, then there
      // is also a raw value. Split the string and capture the right side value
      // and save that as the raw value
      if (date_parts.length() < value.length())
      {
         date_raw = value.substr(value.find(' '), value.length());
         dateJson["raw"] = date_raw;
      }

      // Separate the date parts into their component year, month, and day parts
      // CSL dates are represented as an array of ints representing the
      // [year, month, day]
      // and CSL Date values on CSL object are arrays of these CSL dates (so
      // [[year, month, day]]
      if (date_parts.length() > 0)
      {
         std::vector<std::string> dateParts;
         boost::algorithm::split(dateParts,
                                 date_parts,
                                 boost::algorithm::is_any_of("-"));
         if (dateParts.size() == 3)
         {
            json::Array datePartsJson;
            boost::optional<int> year = safe_convert::stringTo<int>(dateParts.at(0));
            if (year && *year > 0) {
               datePartsJson.push_back(*year);
            }

            boost::optional<int> month = safe_convert::stringTo<int>(dateParts.at(1));
            if (month && *month > 0) {
               datePartsJson.push_back(*month);
            }
            boost::optional<int> day = safe_convert::stringTo<int>(dateParts.at(2));
            if (day && *day > 0) {
               datePartsJson.push_back(*day);
            }

            json::Array datePartContainer;
            datePartContainer.push_back(datePartsJson);
            dateJson["date-parts"] = datePartContainer;
         }
      }

      return dateJson;
   }
   else
   {
      return json::Value(value);
   }
}

// Return a CSL type for a given zoteroType
// The type mappings were derived from the mappings here:
// https://aurimasv.github.io/z2csl/typeMap.xml
std::string cslType(std::string zoteroType) {
   if (zoteroType == "artwork") {
      return "graphic";
   } else if (zoteroType == "attachment") {
      return "article";
   } else if (zoteroType == "audioRecording") {
      return "song";
   } else if (zoteroType == "bill") {
      return "bill";
   } else if (zoteroType == "blogPost") {
      return "post-weblog";
   } else if (zoteroType == "book") {
      return "book";
   } else if (zoteroType == "bookSection") {
      return "chapter";
   } else if (zoteroType == "case") {
      return "legal_case";
   } else if (zoteroType == "computerProgram") {
      return "book";
   } else if (zoteroType == "conferencePaper") {
      return "paper-conference";
   } else if (zoteroType == "dictionaryEntry") {
      return "entry-dictionary";
   } else if (zoteroType == "document") {
      return "article";
   } else if (zoteroType == "email") {
      return "personal_communication";
   } else if (zoteroType == "encyclopediaArticle") {
      return "entry-encyclopedia";
   } else if (zoteroType == "film") {
      return "motion_picture";
   } else if (zoteroType == "forumPost") {
      return "post";
   } else if (zoteroType == "hearing") {
      return "bill";
   } else if (zoteroType == "instantMessage") {
      return "personal_communication";
   } else if (zoteroType == "interview") {
      return "interview";
   } else if (zoteroType == "journalArticle") {
      return "article-journal";
   } else if (zoteroType == "letter") {
      return "personal_communication";
   } else if (zoteroType == "magazineArticle") {
      return "article-magazine";
   } else if (zoteroType == "manuscript") {
      return "manuscript";
   } else if (zoteroType == "map") {
      return "map";
   } else if (zoteroType == "newspaperArticle") {
      return "article-newspaper";
   } else if (zoteroType == "note") {
      return "article";
   } else if (zoteroType == "patent") {
      return "patent";
   } else if (zoteroType == "podcast") {
      return "song";
   } else if (zoteroType == "presentation") {
      return "speech";
   } else if (zoteroType == "radioBroadcast") {
      return "broadcast";
   } else if (zoteroType == "report") {
      return "report";
   } else if (zoteroType == "statute") {
      return "legislation";
   } else if (zoteroType == "thesis") {
      return "thesis";
   } else if (zoteroType == "tvBroadcast") {
      return "broadcast";
   } else if (zoteroType == "videoRecording") {
      return "motion_picture";
   } else if (zoteroType == "webpage") {
      return "webpage";
   } else {
      return zoteroType;
   }
}


} // end anonymous namespace

// Convert the items and creators read from SQLite into a CSL Object
json::Object sqliteItemToCSL(std::map<std::string,std::string> item, const ZoteroCreatorsByKey& creators)
{
   json::Object cslJson;

   // Type is a special global field (all items are required to have an
   // type property). Map the zotero type to it's corresponding CSL
   // type and save that to the JSON
   const std::string zoteroType = item["type"];
   if (!zoteroType.empty()) {

      // Get the field mapping for this type
      std::map<std::string, std::string> cslFieldsNames = cslFieldNames(zoteroType);
      cslJson["type"] = cslType(zoteroType);

      // Process the fields. This will either just apply the field
      // as if to the json or will transform the name and/or value
      // before applying the field to the json.
      for (auto field : item)
      {
         const std::string zoteroFieldName = field.first;
         const std::string fieldValue = field.second;

         // Type is a special global property that is used to deduce the
         // right name mapping for properties, so it is written above.
         // Just skip it when writing the fields.
         if (zoteroFieldName != "type") {
            // write the value
            std:: string fieldName = zoteroFieldName;
            const std::string cslName = cslFieldsNames[zoteroFieldName];
            if (cslName.length() > 0) {
               fieldName = cslName;
            }
            cslJson[fieldName] = transformValue(fieldName, fieldValue);
         }
      }

      // get the item creators
      ZoteroCreatorsByKey::const_iterator it = creators.find(item["key"]);
      if (it != creators.end())
      {
         std::map<std::string,json::Array> creatorsByType;
         std::for_each(it->second.begin(), it->second.end(), [&creatorsByType](const ZoteroCreator& creator) {

           if (!creator.creatorType.empty())
           {
              json::Object jsonCreator;
              if (!creator.firstName.empty())
                 jsonCreator["given"] = creator.firstName;
              if (!creator.lastName.empty())
                 jsonCreator["family"] = creator.lastName;

              creatorsByType[creator.creatorType].push_back(jsonCreator);
           }
         });

         // set author
         for (auto typeCreators : creatorsByType)
         {
            // Creators need to be mapped to the appropriate
            // CSL property. Most just pass through, but there are
            // some special cases.
            //
            // The following CSL creator types have no corresponding
            // Zotero fields and so should never be emitted:
            //     editorial-director
            //     illustrator
            //     original-author
            std::string zoteroCreatorType = typeCreators.first;
            json::Array creatorsArray = typeCreators.second;

            std:: string fieldName = zoteroCreatorType;
            const std::string cslName = cslFieldsNames[zoteroCreatorType];
            if (cslName.length() > 0) {
               fieldName = cslName;
            }
            cslJson[fieldName] = creatorsArray;
         }
      }
   }
   return cslJson;
}


} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
