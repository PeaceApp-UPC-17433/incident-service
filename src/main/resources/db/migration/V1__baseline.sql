CREATE TABLE public.evidences (
    id uuid NOT NULL,
    media_type character varying(255) NOT NULL,
    size_bytes bigint NOT NULL,
    url character varying(255) NOT NULL,
    incident_id uuid NOT NULL,
    CONSTRAINT evidences_media_type_check CHECK (((media_type)::text = ANY ((ARRAY['FOTO'::character varying, 'VIDEO'::character varying])::text[])))
);

CREATE TABLE public.incident_audit_logs (
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    incident_id uuid NOT NULL,
    performed_at timestamp(6) with time zone NOT NULL,
    performed_by uuid NOT NULL,
    CONSTRAINT incident_audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['CREATED'::character varying, 'UPDATED'::character varying, 'DELETED'::character varying])::text[])))
);

CREATE TABLE public.incidents (
    id uuid NOT NULL,
    anonymous boolean NOT NULL,
    author_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(1000),
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT incidents_status_check CHECK (((status)::text = ANY ((ARRAY['PENDIENTE_VALIDACION'::character varying, 'VALIDADO'::character varying, 'DESCARTADO'::character varying, 'EN_MODERACION'::character varying])::text[]))),
    CONSTRAINT incidents_type_check CHECK (((type)::text = ANY ((ARRAY['ROBO'::character varying, 'HURTO'::character varying, 'ACCIDENTE_TRANSITO'::character varying, 'AGRESION'::character varying, 'VANDALISMO'::character varying, 'OTRO'::character varying])::text[])))
);

CREATE TABLE public.outbox_events (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    payload text NOT NULL,
    published_at timestamp(6) with time zone,
    routing_key character varying(100) NOT NULL
);

ALTER TABLE ONLY public.evidences
    ADD CONSTRAINT evidences_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.incident_audit_logs
    ADD CONSTRAINT incident_audit_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.evidences
    ADD CONSTRAINT fk24t50n30arepix0w1jnfuo339 FOREIGN KEY (incident_id) REFERENCES public.incidents(id);
