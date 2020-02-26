CREATE TABLE RevokedCookie(
   Expiration text NOT NULL,
   CookieData text NOT NULL,
   PRIMARY KEY (Expiration, CookieData)
);
