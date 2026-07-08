SELECT
    owner_name,
    owner_email,
    COUNT(*)                                                        AS total_tasks,
    COUNTIF(status = 'done')                                        AS done_tasks,
    COUNTIF(status = 'pending')                                     AS pending_tasks,
    COUNTIF(status = 'in_progress')                                 AS in_progress_tasks,
    COUNTIF(ai_analysed = true)                                     AS ai_analysed_tasks,
    ROUND(COUNTIF(status = 'done') * 100.0 / COUNT(*), 1)          AS done_pct,
    ROUND(COUNTIF(ai_analysed = true) * 100.0 / COUNT(*), 1)       AS ai_analysed_pct,
    ROUND(COUNTIF(priority = 'high') * 100.0 / COUNT(*), 1)        AS high_priority_pct,
    ROUND(AVG(days_open), 1)                                        AS avg_days_open
FROM {{ ref('stg_tasks') }}
GROUP BY owner_name, owner_email
ORDER BY total_tasks DESC
