USE sanaesencia_reservas;
SHOW TABLES;
SELECT id, nombre, activo FROM boxes;
SELECT id, paciente_id, especialista_id, servicio_id, inicio, fin, modalidad, estado, box_id FROM reservas ORDER BY id;
SELECT estado, COUNT(*) AS cantidad FROM reservas GROUP BY estado;
