create table frames (
    frame_id varchar(64) not null,
    media_type varchar(16) not null,
    format varchar(100) not null,
    environment varchar(32) not null,
    site_number varchar(64),
    station varchar(150),
    address varchar(500),
    region varchar(100),
    country_code varchar(8) not null,
    town varchar(150),
    postcode varchar(16),
    longitude decimal(10, 7),
    latitude decimal(10, 7),
    status varchar(32) not null,
    status_reason varchar(255),
    number_of_slots integer,
    distance_to_closest_school integer,
    pixel_height integer,
    pixel_width integer,
    premium boolean not null default false,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint not null default 0,
    primary key (frame_id),
    index idx_frames_status (status),
    index idx_frames_environment (environment),
    index idx_frames_media_type (media_type),
    index idx_frames_updated_at (updated_at, frame_id)
);

create table frame_revisions (
    id bigint not null auto_increment,
    frame_id varchar(64) not null,
    action varchar(32) not null,
    source varchar(32) not null,
    actor varchar(100) not null,
    occurred_at timestamp(6) not null,
    primary key (id),
    constraint fk_frame_revisions_frame foreign key (frame_id) references frames (frame_id),
    index idx_frame_revisions_frame_time (frame_id, occurred_at)
);

create table frame_revision_changes (
    id bigint not null auto_increment,
    revision_id bigint not null,
    field_name varchar(64) not null,
    old_value text,
    new_value text,
    primary key (id),
    constraint fk_revision_changes_revision foreign key (revision_id) references frame_revisions (id) on delete cascade,
    index idx_revision_changes_revision (revision_id)
);
