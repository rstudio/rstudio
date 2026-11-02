/*
 * SslTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <memory>
#include <string>
#include <vector>

#include <openssl/bio.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/pem.h>

#include <core/http/Ssl.hpp>
#include <core/system/Crypto.hpp>
#include <shared_core/FilePath.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace ssl {
namespace tests {

using Names = std::vector<std::string>;

// the nginx default cipher list
const std::string kCiphers = "HIGH:!aNULL:!MD5";

// RSA key generation is slow, so all tests share one certificate and key
class SslProtocolSupportTest : public ::testing::Test
{
protected:
   static void SetUpTestSuite()
   {
      ASSERT_FALSE(FilePath::tempFilePath(dir_));
      ASSERT_FALSE(dir_.ensureDirectory());
      certificate_ = dir_.completeChildPath("server.crt");
      key_ = dir_.completeChildPath("server.key");
      ASSERT_FALSE(core::system::crypto::generateRsaCertAndKeyFiles("localhost", certificate_, key_));
   }

   static void TearDownTestSuite()
   {
      dir_.removeIfExists();
   }

   static FilePath dir_;
   static FilePath certificate_;
   static FilePath key_;
};

FilePath SslProtocolSupportTest::dir_;
FilePath SslProtocolSupportTest::certificate_;
FilePath SslProtocolSupportTest::key_;

TEST_F(SslProtocolSupportTest, ModernProtocolsAreUsable)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("TLSv1.2 TLSv1.3", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.2", "TLSv1.3" }));
   EXPECT_TRUE(support.unusable.empty());
}

TEST_F(SslProtocolSupportTest, ProtocolMissingFromOpenSslIsUnusable)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("SSLv2 TLSv1.2", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.2" }));
   EXPECT_EQ(support.unusable, (Names{ "SSLv2" }));
}

// OpenSSL 1.1.0 and later leave SSLv3 out of the default build, so the
// handshake itself fails
TEST_F(SslProtocolSupportTest, ProtocolWithFailedHandshakeIsUnusable)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("SSLv3 TLSv1.3", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.3" }));
   EXPECT_EQ(support.unusable, (Names{ "SSLv3" }));
}

// a failed handshake leaves entries on the OpenSSL error queue, and the next
// handshake clears them, so the failed handshake comes last here
TEST_F(SslProtocolSupportTest, ErrorQueueIsClearAfterFailedHandshake)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("TLSv1.3 SSLv3", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.unusable, (Names{ "SSLv3" }));
   EXPECT_EQ(0UL, ERR_peek_error());
}

TEST_F(SslProtocolSupportTest, NamesMatchWithoutRegardToCase)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("tlsv1.2 TLSV1.3", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.2", "TLSv1.3" }));
   EXPECT_TRUE(support.unusable.empty());
}

TEST_F(SslProtocolSupportTest, RepeatedNamesAreReportedOnce)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("SSLv2 TLSv1.3 sslv2 TLSv1.3", certificate_, key_, kCiphers,
                                     &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.3" }));
   EXPECT_EQ(support.unusable, (Names{ "SSLv2" }));
}

TEST_F(SslProtocolSupportTest, UnknownNamesAndExtraSpacesAreSkipped)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport(" TLSv9  TLSv1.3 ", certificate_, key_, kCiphers, &support));
   EXPECT_EQ(support.usable, (Names{ "TLSv1.3" }));
   EXPECT_TRUE(support.unusable.empty());
}

TEST_F(SslProtocolSupportTest, EmptyListReportsNothing)
{
   ProtocolSupport support;
   ASSERT_FALSE(checkProtocolSupport("", certificate_, key_, kCiphers, &support));
   EXPECT_TRUE(support.usable.empty());
   EXPECT_TRUE(support.unusable.empty());
}

TEST_F(SslProtocolSupportTest, InvalidCipherListIsAnError)
{
   ProtocolSupport support;
   Error error = checkProtocolSupport("TLSv1.2", certificate_, key_, "NO-SUCH-CIPHER", &support);
   EXPECT_TRUE(error);
   EXPECT_EQ("NO-SUCH-CIPHER", error.getProperty("cipher-list"));
   EXPECT_TRUE(support.usable.empty());
}

TEST_F(SslProtocolSupportTest, MissingKeyIsAnError)
{
   ProtocolSupport support;
   FilePath missingKey = dir_.completeChildPath("missing.key");
   EXPECT_TRUE(checkProtocolSupport("TLSv1.2", certificate_, missingKey, kCiphers, &support));
   EXPECT_TRUE(support.usable.empty());
}

TEST_F(SslProtocolSupportTest, KeyForAnotherCertificateIsAnError)
{
   FilePath otherCertificate = dir_.completeChildPath("other.crt");
   FilePath otherKey = dir_.completeChildPath("other.key");
   ASSERT_FALSE(core::system::crypto::generateRsaCertAndKeyFiles("other", otherCertificate, otherKey));

   ProtocolSupport support;
   EXPECT_TRUE(checkProtocolSupport("TLSv1.2", certificate_, otherKey, kCiphers, &support));
}

TEST_F(SslProtocolSupportTest, EncryptedKeyIsAnError)
{
   std::unique_ptr<BIO, decltype(&BIO_free)> plainBio(
      BIO_new_file(key_.getAbsolutePath().c_str(), "r"), BIO_free);
   ASSERT_TRUE(plainBio);
   std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)> key(
      PEM_read_bio_PrivateKey(plainBio.get(), nullptr, nullptr, nullptr), EVP_PKEY_free);
   ASSERT_TRUE(key);

   FilePath encryptedKey = dir_.completeChildPath("encrypted.key");
   std::unique_ptr<BIO, decltype(&BIO_free)> encryptedBio(
      BIO_new_file(encryptedKey.getAbsolutePath().c_str(), "w"), BIO_free);
   ASSERT_TRUE(encryptedBio);
   std::string passphrase = "secret";
   ASSERT_EQ(1, PEM_write_bio_PrivateKey(encryptedBio.get(), key.get(), EVP_aes_256_cbc(),
                                         reinterpret_cast<unsigned char*>(&passphrase[0]),
                                         static_cast<int>(passphrase.size()),
                                         nullptr, nullptr));
   encryptedBio.reset();

   ProtocolSupport support;
   EXPECT_TRUE(checkProtocolSupport("TLSv1.2", certificate_, encryptedKey, kCiphers, &support));
}

} // namespace tests
} // namespace ssl
} // namespace http
} // namespace core
} // namespace rstudio
