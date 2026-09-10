import type { NextConfig } from "next";
const commit = process.env.VERCEL_GIT_COMMIT_SHA || process.env.GITHUB_SHA || "local";
const releaseId = commit === "local" ? "local" : commit.slice(0, 7);
const nextConfig: NextConfig = {
  images: { remotePatterns: [{ protocol: "https", hostname: "*.supabase.co" }] },
  env: { NEXT_PUBLIC_RELEASE_ID: releaseId },
};
export default nextConfig;
