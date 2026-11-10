-- Lab 10, task 2: users and roles live in the database, passwords are stored as BCrypt hashes.
-- A new migration file; applied migrations are never edited.
create table app_user (
    id            bigserial primary key,
    username      varchar(60)  not null unique,
    password_hash varchar(100) not null,     -- BCrypt is 60 characters
    enabled       boolean      not null default true
);

create table user_role (
    user_id bigint      not null references app_user (id),
    role    varchar(20) not null,
    primary key (user_id, role)
);

-- two users; the hashes are of user123 and admin123
-- (generated with new BCryptPasswordEncoder().encode(...), see logs/lab10/04_task2_bcrypt_hashes.txt)
insert into app_user (username, password_hash) values
    ('user',  '$2a$10$bn8Yvibxl7GD.gzYK5o7E.987sMHKL09.4GpeBvvyWUr.K3qeR.lG'),
    ('admin', '$2a$10$5XcixIsZQJDemkVlLTZ3XuXd8rRpOSeARWr1JfKjowRBm7Ah5aB8e');

insert into user_role (user_id, role)
    select id, 'USER'  from app_user;
insert into user_role (user_id, role)
    select id, 'ADMIN' from app_user where username = 'admin';
