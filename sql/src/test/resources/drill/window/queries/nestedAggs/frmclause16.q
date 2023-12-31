SELECT 
    AVG(SUM(c3)) OVER(PARTITION BY c8 ORDER BY c1 RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING), c8 
from ( SELECT * from "t_alltype.parquet" ) GROUP BY c1, c8
