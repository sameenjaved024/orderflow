-- Creates a separate database for each service
-- Runs automatically when the postgres container first starts
CREATE DATABASE order_db;
CREATE DATABASE fulfillment_db;
CREATE DATABASE notification_db;