SELECT
    id,
    title,
    description,
    LOWER(priority)  AS priority,
    LOWER(status)    AS status,
    LOWER(category)  AS category,
    ai_summary,
    ai_analysed,
    owner_name,
    owner_email,
    created_at,
    updated_at,
    TIMESTAMP_DIFF(updated_at, created_at, DAY) AS days_open
FROM {{ source('smartops_analytics', 'tasks_raw') }}
