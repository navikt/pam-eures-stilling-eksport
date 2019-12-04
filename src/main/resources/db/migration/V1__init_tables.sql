create table stillinger(
	id varchar(128) not null primary key,
    kilde varchar(200) not null, -- Skal vi bruke NAV her, eller virkelig kilde???
    status varchar(100) not null,
    opprettet_ts timestamp not null,
    sist_endret_ts timestamp not null,
    lukket_ts timestamp null,
	json_stilling text not null
);

create table feedpeker(
    sist_lest varchar(100) not null
);

CREATE TABLE shedlock(
    name VARCHAR(64),
    lock_until TIMESTAMP(3) NULL,
    locked_at TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255),
    PRIMARY KEY (name)
);
