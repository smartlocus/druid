SELECT 
    RANK() OVER (ORDER BY c1 DESC) as w_rnk,
    AVG(c2) OVER (PARTITION BY c8 ORDER BY 1 ASC NULLS LAST RANGE BETWEEN CURRENT ROW AND CURRENT ROW) as w_avg
FROM "t_alltype.parquet"
    WINDOW w AS (PARTITION BY c8 ORDER BY c1 DESC NULLS FIRST RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
