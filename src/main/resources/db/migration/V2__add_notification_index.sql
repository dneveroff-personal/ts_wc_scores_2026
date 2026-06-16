-- Ускоряет запрос "кто не сделал прогноз на матч X"
-- используется в AppScheduler.sendReminders()
CREATE INDEX idx_predictions_match_user ON predictions (match_id, user_id);

-- Ускоряет сортировку лидерборда
CREATE INDEX idx_users_total_points ON users (total_points DESC);
