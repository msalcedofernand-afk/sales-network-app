-- Enable pg_cron extension if not already enabled
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Schedule daily purge of expired invitations at 3am UTC
SELECT cron.schedule(
  'purge-expired-invitations',
  '0 3 * * *',
  $$
  UPDATE public.invitations
  SET status = 'EXPIRED'
  WHERE expires_at <= now()
    AND status = 'ACTIVE';
  $$
);

-- Schedule cleanup of old crash reports (keep 30 days)
SELECT cron.schedule(
  'cleanup-old-crash-reports',
  '0 4 * * *',
  $$
  DELETE FROM public.crash_reports
  WHERE created_at < now() - interval '30 days';
  $$
);
