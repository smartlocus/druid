SELECT c2, COUNT(MIN(c2)) OVER ( PARTITION BY c2 ) FROM "tblWnulls.parquet" GROUP BY c2
