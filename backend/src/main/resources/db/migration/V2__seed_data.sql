-- Demonstration data so a fresh checkout has something to look at.
-- The credentials below are throwaway demo logins, not secrets.

insert into site (code, name, location) values
    ('BKN-N', 'Bakken North Gathering', 'Williston Basin, ND'),
    ('PRM-W', 'Permian West Battery',   'Midland Basin, TX'),
    ('GLF-3', 'Gulf Platform 3',        'Offshore, LA');

insert into asset (tag, name, manufacturer, model, site_id) values
    ('PMP-001', 'Crude transfer pump',   'Sulzer',        'MSD-4x6',   (select id from site where code = 'BKN-N')),
    ('CMP-002', 'Gas lift compressor',   'Ariel',         'JGK-2',     (select id from site where code = 'BKN-N')),
    ('SEP-003', 'Three phase separator', 'Cameron',       'NATCO-72',  (select id from site where code = 'PRM-W')),
    ('PMP-004', 'Water injection pump',  'Flowserve',     'HPX-6',     (select id from site where code = 'PRM-W')),
    ('GEN-005', 'Standby generator',     'Caterpillar',   'C18',       (select id from site where code = 'GLF-3'));

insert into technician (employee_no, full_name, email, specialty) values
    ('T-1001', 'Dana Okoro',    'dana.okoro@example.com',    'Rotating equipment'),
    ('T-1002', 'Miguel Santos', 'miguel.santos@example.com', 'Instrumentation'),
    ('T-1003', 'Priya Nair',    'priya.nair@example.com',    'Electrical'),
    ('T-1004', 'Chen Wei',      'chen.wei@example.com',      'Hydraulics');

insert into part (sku, name, quantity_in_stock, version) values
    ('SEAL-101', 'Mechanical seal kit 4in',  24, 0),
    ('BRG-204',  'Thrust bearing assembly',  12, 0),
    ('FLT-330',  'Inlet filter element',     60, 0),
    ('VLV-412',  'Relief valve 150psi',       8, 0),
    ('GSK-505',  'Flange gasket set',        45, 0),
    ('SNS-610',  'Pressure transmitter',      3, 0);

insert into work_order
    (reference, title, description, status, priority, asset_id, technician_id, opened_at, closed_at, version)
values
    ('WO-000001', 'Excessive vibration on start-up',
     'Operator reports vibration above trip threshold during cold start.',
     'IN_PROGRESS', 'HIGH',
     (select id from asset where tag = 'PMP-001'),
     (select id from technician where employee_no = 'T-1001'),
     now() - interval '11 days', null, 0),

    ('WO-000002', 'Discharge pressure below setpoint',
     'Compressor discharge holding 15% under setpoint since last shutdown.',
     'ASSIGNED', 'CRITICAL',
     (select id from asset where tag = 'CMP-002'),
     (select id from technician where employee_no = 'T-1004'),
     now() - interval '4 days', null, 0),

    ('WO-000003', 'Level transmitter reading erratic',
     'Intermittent spikes on the interface level signal.',
     'OPEN', 'MEDIUM',
     (select id from asset where tag = 'SEP-003'),
     null,
     now() - interval '18 days', null, 0),

    ('WO-000004', 'Quarterly seal inspection',
     'Scheduled preventive maintenance.',
     'CLOSED', 'LOW',
     (select id from asset where tag = 'PMP-004'),
     (select id from technician where employee_no = 'T-1002'),
     now() - interval '30 days', now() - interval '26 days', 0),

    ('WO-000005', 'Generator fails to hold load',
     'Drops offline roughly two minutes after transfer.',
     'OPEN', 'HIGH',
     (select id from asset where tag = 'GEN-005'),
     null,
     now() - interval '2 days', null, 0);

insert into work_order_part (work_order_id, part_id, quantity) values
    ((select id from work_order where reference = 'WO-000001'),
     (select id from part where sku = 'SEAL-101'), 2),
    ((select id from work_order where reference = 'WO-000001'),
     (select id from part where sku = 'GSK-505'), 1),
    ((select id from work_order where reference = 'WO-000004'),
     (select id from part where sku = 'SEAL-101'), 1);

-- Keep the generator ahead of the references seeded above.
select setval('work_order_reference_seq', 5);

-- Demo logins. Passwords: admin123 / super123 / tech123.
insert into app_user (username, password_hash, role, enabled) values
    ('admin',      '$2a$10$T6iuUBqNPW4Dm3EnL39oku508XYP0fVuwp2eQQg88PS7Og0cXQ.w.', 'ADMIN',      true),
    ('supervisor', '$2a$10$eq.a7ASvhmHZ/46BEwz3XO7VNb8jDxU3zqCPyOsJLT2Ch6LrnI50S', 'SUPERVISOR', true),
    ('tech',       '$2a$10$7HJDfpKwbkNjm4CIQY0RRek/sbv8W8/w4WHaDbQ0ARq.rBTMuRs0u', 'TECHNICIAN', true);
