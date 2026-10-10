CREATE UNIQUE INDEX uq_users_email_case_insensitive ON users (lower(email));
