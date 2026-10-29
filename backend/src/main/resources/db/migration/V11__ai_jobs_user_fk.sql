-- ai_jobs 에 users FK 가 빠져 있어 탈퇴해도 붙여넣은 코드(user_prompt)가 남았다.
-- 다른 유저 소유 테이블과 같이 ON DELETE CASCADE → 탈퇴 시 AI 요청 기록도 자동 정리
-- FK 를 걸기 전에 이미 주인이 없는 행을 먼저 지운다(없으면 0건)
delete from ai_jobs where user_id not in (select id from users);

alter table ai_jobs
    add constraint fk_ai_jobs_user foreign key (user_id) references users(id) on delete cascade;
