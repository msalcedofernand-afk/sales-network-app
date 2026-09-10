-- Create crash_reports table for Android crash logging
CREATE TABLE IF NOT EXISTS public.crash_reports (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  app_version text NOT NULL,
  version_code integer NOT NULL,
  build_type text NOT NULL DEFAULT 'debug',
  stacktrace text NOT NULL,
  device_info jsonb NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.crash_reports ENABLE ROW LEVEL SECURITY;

-- Only service role can insert (from Edge Functions)
CREATE POLICY crash_reports_insert_service ON public.crash_reports
  FOR INSERT WITH CHECK (auth.role() = 'service_role');

-- Only authenticated users can read their own crashes
CREATE POLICY crash_reports_read_own ON public.crash_reports
  FOR SELECT USING (auth.uid() = user_id);

-- Leaders can read crashes from their team members
CREATE POLICY crash_reports_read_leader ON public.crash_reports
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.team_members tm
      WHERE tm.team_id IN (
        SELECT tm2.team_id FROM public.team_members tm2
        WHERE tm2.user_id = auth.uid() AND tm2.role IN ('LIDER', 'ROOT_ADMIN')
      )
      AND tm.user_id = crash_reports.user_id
    )
  );

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_crash_reports_created_at ON public.crash_reports(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crash_reports_user_id ON public.crash_reports(user_id);
CREATE INDEX IF NOT EXISTS idx_crash_reports_version ON public.crash_reports(app_version);
