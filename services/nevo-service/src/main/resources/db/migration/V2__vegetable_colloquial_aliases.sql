-- V2__vegetable_colloquial_aliases.sql
-- NL colloquial / EN variants → terms that hit NEVO EN names / search_document.
-- Keep small and extensible; do not dump a full bilingual dictionary.

INSERT INTO nevo_alias (id, alias_term, canonical_term) VALUES
  ('22222222-2222-2222-2222-222222222201', 'paprika', 'sweet pepper'),
  ('22222222-2222-2222-2222-222222222202', 'bell pepper', 'sweet pepper'),
  ('22222222-2222-2222-2222-222222222203', 'zucchini', 'courgette'),
  ('22222222-2222-2222-2222-222222222204', 'courgettes', 'courgette'),
  ('22222222-2222-2222-2222-222222222205', 'eggplant', 'aubergine');
