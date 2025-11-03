-- 添加login_time字段到sys_user表
ALTER TABLE sys_user ADD COLUMN login_time datetime NULL COMMENT '最后登录时间';

-- 可选：为login_time字段添加索引以提高查询性能
CREATE INDEX idx_sys_user_login_time ON sys_user(login_time);