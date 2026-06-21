-- Add rule_type column to survey_visibility_rule and migrate existing logic_group data
ALTER TABLE survey_visibility_rule ADD COLUMN rule_type VARCHAR(10) NOT NULL DEFAULT 'AND' AFTER logic_group;
UPDATE survey_visibility_rule SET rule_type = CASE WHEN logic_group = '0' THEN 'AND' ELSE 'OR' END;
ALTER TABLE survey_visibility_rule DROP COLUMN logic_group;
