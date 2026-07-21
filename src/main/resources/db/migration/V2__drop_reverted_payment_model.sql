-- Limpeza do modelo de planos/pagamento (FREE/PLUS, InfinitePay) que foi implementado
-- e depois revertido por completo quando o projeto virou peca de portfolio local.
-- IF EXISTS pra nao quebrar num banco novo, onde essas colunas/tabela nunca existiram.
ALTER TABLE account DROP COLUMN IF EXISTS plan;
ALTER TABLE account DROP COLUMN IF EXISTS last_search_at;
DROP TABLE IF EXISTS pending_payment;
