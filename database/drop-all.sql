-- Drops every table, procedure, function and trigger owned by the application user.
-- Used by reset-db.sh to rebuild the database from scratch.
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR o IN (SELECT object_name, object_type FROM user_objects
               WHERE object_type IN ('PROCEDURE', 'FUNCTION', 'VIEW')) LOOP
        EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' ' || o.object_name;
    END LOOP;
END;
/
