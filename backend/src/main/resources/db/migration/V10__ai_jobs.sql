-- AI 분석/힌트 비동기 처리용 잡 테이블.
-- 요청 시 PENDING 으로 저장 → Kafka 컨슈머가 처리 후 DONE/FAILED 로 갱신.
create table ai_jobs (
    id            bigserial   primary key,
    user_id       bigint      not null,
    kind          varchar(20) not null,        -- ANALYZE / HINT
    status        varchar(20) not null,        -- PENDING / DONE / FAILED
    system_prompt text        not null,
    user_prompt   text        not null,
    result        text,
    error_code    varchar(40),
    created_at    timestamp   not null default now(),
    updated_at    timestamp   not null default now()
);

create index idx_ai_jobs_user_id on ai_jobs (user_id);
