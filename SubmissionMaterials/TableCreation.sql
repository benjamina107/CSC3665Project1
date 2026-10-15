
create table employees(
eid integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
ename text NOT NULL,
epass text NOT NULL,
euser text NOT NULL,
sick_days int default 4,
vacation_days int default 8,
etype text not null,
CHECK (etype SIMILAR TO '(Doctor|Technician|Admin)'),
first_login boolean DEFAULT TRUE
);


create table shifts(
surgery boolean NOT NULL,
block text not null,
CHECK (block SIMILAR TO '(Early Morning|8:30 Shift|Late Doctor|Sunday Shift|Overnight)'),
day_of_week text not null,
CHECK (day_of_week SIMILAR TO '(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)'),
sdate date not null,
primary key (block, sdate)
);


create table requests(
rid integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
start_date DATE NOT NULL,
end_date DATE NOT NUll,
approved boolean default false,
eid integer references employees(eid),
request_timestamp timestamp not null
);

create table pto_requests(
rid int primary key references requests(rid) on delete cascade,
reason text not null,
CHECK (reason SIMILAR TO '(Sick|Vacation)')
);


create table work_requests(
rid int primary key references requests(rid) on delete cascade,
sdate date,
block text,
CONSTRAINT fk_shifts
	foreign key (sdate, block)
	REFERENCES shifts(sdate, block)
);

create table working(
eid integer references employees(eid),
sdate date,
block text,
CONSTRAINT fk_shifts
	foreign key (sdate, block)
	REFERENCES shifts(sdate, block)
);
