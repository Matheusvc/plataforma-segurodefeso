ALTER TABLE clientes ADD COLUMN cpf_conferido_em  DATE;
ALTER TABLE clientes ADD COLUMN cpf_conferido_por BIGINT;
ALTER TABLE clientes ADD CONSTRAINT fk_cliente_cpf_conferido_por FOREIGN KEY (cpf_conferido_por) REFERENCES usuarios (id);
