SELECT c2, SUM(COUNT(c1)) OVER ( PARTITION BY c2 ORDER BY c2 ) FROM "tblWnulls.parquet" GROUP BY c2
