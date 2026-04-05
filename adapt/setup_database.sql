-- ADAPT Project - Database Setup Script
-- Run this as the postgres user: psql -U postgres -f setup_database.sql

-- Create the database
CREATE DATABASE adapt_db;

-- Connect to the new database (this requires running in separate psql session)
-- For now, we'll switch context and create the user in the new database

-- Create the user
CREATE USER adapt_user WITH PASSWORD 'adapt_password';

-- Grant privileges on database
GRANT ALL PRIVILEGES ON DATABASE adapt_db TO adapt_user;

-- Connect to adapt_db and set permissions
\c adapt_db;

-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO adapt_user;
GRANT CREATE ON SCHEMA public TO adapt_user;

-- Alter user role settings
ALTER ROLE adapt_user SET client_encoding TO 'utf8';
ALTER ROLE adapt_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE adapt_user SET default_transaction_deferrable TO off;
ALTER ROLE adapt_user SET default_transaction_read_only TO off;
ALTER ROLE adapt_user SET timezone TO 'UTC';

-- Verify
SELECT datname FROM pg_database WHERE datname = 'adapt_db';
SELECT usename FROM pg_user WHERE usename = 'adapt_user';

\echo 'Database setup complete!'
