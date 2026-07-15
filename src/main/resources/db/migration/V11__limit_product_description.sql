UPDATE product SET description = '' WHERE description IS NULL;
UPDATE product SET description = LEFT(description, 50) WHERE CHAR_LENGTH(description) > 50;
ALTER TABLE product ALTER COLUMN description TYPE VARCHAR(50);
ALTER TABLE product ALTER COLUMN description SET DEFAULT '';
ALTER TABLE product ALTER COLUMN description SET NOT NULL;
