ALTER TABLE users
    ADD COLUMN checkout_bypass_permitted BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.checkout_bypass_permitted IS
    'Explicit checkout restriction bypass grant. False is not a general purchase denial.';
