-- 1696293426 DOWN parcels-db-indexes
DROP INDEX parcels_parcels_status_created_at_idx ON parcels;
DROP INDEX parcels_prepaid_parcels_status_created_at_idx ON parcels_prepaid;
DROP INDEX parcels_prepaid_parcels_status_idx ON parcels_prepaid;
DROP INDEX parcels_prepaid_created_at_idx ON parcels_prepaid;