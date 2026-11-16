-- Clientes existentes e importados contam como cadastro concluído; só o assistente cria cadastros em andamento.
ALTER TABLE clientes ADD COLUMN cadastro_concluido BOOLEAN DEFAULT TRUE NOT NULL;
