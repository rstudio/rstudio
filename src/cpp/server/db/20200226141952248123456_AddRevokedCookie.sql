/* Stores revoked auth cookies - cookies that are not yet expired, but
   have been invalidated due to a user signing out. These cookies will
   not be allowed to be used again for sign-in purposes.
*/
CREATE TABLE RevokedCookie(
   /* The expiration date string at which this cookie expires. Derivative of the CookieData,
      used for sorting purposes
   */
   Expiration text NOT NULL,
   
   /* The actual cookie data */
   CookieData text NOT NULL,

   /* Primary key is the actual cookie data itself (no duplicate cookies can be issued) */
   PRIMARY KEY (CookieData)
);

/* Index to be used for sorting revoked cookies by expiration */
CREATE INDEX RevokedCookieExpirationIndex ON RevokedCookie(Expiration);
