import type { NextConfig } from "next";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
const commit = process.env.VERCEL_GIT_COMMIT_SHA || process.env.GITHUB_SHA || "local";
const releaseId = commit === "local" ? "local" : commit.slice(0, 7);
const deploymentId = process.env.VERCEL_DEPLOYMENT_ID || process.env.GITHUB_RUN_NUMBER || "local";
const props = Object.fromEntries(readFileSync(resolve(process.cwd(), "../version.properties"), "utf8")
  .split(/\r?\n/).filter((line) => line.includes("=") && !line.startsWith("#"))
  .map((line) => line.split("=", 2)));
const channel = (process.env.VERCEL_GIT_COMMIT_REF || process.env.GITHUB_REF_NAME) === "main" ? "stable" : "beta";
const version = channel === "beta" ? `${props.versionName}-beta.${props.betaNumber}` : props.versionName;
const nextConfig: NextConfig = {
  images: { remotePatterns: [{ protocol: "https", hostname: "*.supabase.co" }] },
  env: { NEXT_PUBLIC_RELEASE_ID: releaseId, NEXT_PUBLIC_BUILD_ID: `${releaseId}.${deploymentId}`, NEXT_PUBLIC_APP_VERSION: version, NEXT_PUBLIC_RELEASE_CHANNEL: channel },
};
export default nextConfig;
