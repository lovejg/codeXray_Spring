#!/usr/bin/env bash
docker exec codexray-db psql -U postgres -d codexray -c "
SELECT
  tc.table_name AS child_table,
  ccu.table_name AS parent_table
FROM information_schema.table_constraints tc
JOIN information_schema.constraint_column_usage ccu
  ON tc.constraint_name = ccu.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND ccu.table_name = 'users'
ORDER BY child_table;
"
echo '=== does problems reference users? ==='
docker exec codexray-db psql -U postgres -d codexray -c "
SELECT tc.table_name AS child, ccu.table_name AS parent
FROM information_schema.table_constraints tc
JOIN information_schema.constraint_column_usage ccu
  ON tc.constraint_name = ccu.constraint_name
WHERE tc.constraint_type='FOREIGN KEY' AND tc.table_name='problems';
"
