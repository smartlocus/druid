SELECT col8 , LAG(col8,1) OVER ( PARTITION BY col2 ORDER BY col2,col8 ) LAG_col8 FROM "fewRowsAllData.parquet" ORDER BY col8 FETCH FIRST 10 ROWS ONLY
