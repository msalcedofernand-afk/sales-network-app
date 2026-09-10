const CONFIGURED_VERSION = process.env.NEXT_PUBLIC_APP_VERSION || "1.2.0";
const BUILD_ID = process.env.NEXT_PUBLIC_BUILD_ID || process.env.NEXT_PUBLIC_RELEASE_ID || "local";
const DEPLOYED_CHANNEL = process.env.NEXT_PUBLIC_RELEASE_CHANNEL || "beta";

export default function ReleaseBadge() {
  const isStable = DEPLOYED_CHANNEL === "stable";
  const version = `${CONFIGURED_VERSION} · build ${BUILD_ID}`;
  return <Link href="/cambios" className={`release-badge ${isStable ? "release-stable" : "release-beta"}`} title={`Ver historial: ${version}`}><span className="release-dot" aria-hidden="true" />{isStable ? "Producción" : "Beta"} · v{version}</Link>;
}
import Link from "next/link";
