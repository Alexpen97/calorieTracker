-- Creates one database per service for the local Docker Compose stack.
-- Mounted into /docker-entrypoint-initdb.d/ of the postgres container.
CREATE DATABASE food_catalog;
CREATE DATABASE diary;
CREATE DATABASE users;
CREATE DATABASE nevo;
