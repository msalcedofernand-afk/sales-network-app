"use client";

import { useEffect, useState } from "react";

const CONFIGURED_VERSION = process.env.NEXT_PUBLIC_APP_VERSION;
const RELEASE_ID = process.env.NEXT_PUBLIC_RELEASE_ID || "local";

function initialChannel() {
  if (typeof window === "undefined") return "beta";
  return window.location.hostname === "sales-network-app.vercel.app" ? "stable" : "beta";
}

export default function ReleaseBadge() {
  const [channel, setChannel] = useState(initialChannel);
  useEffect(() => {
    setChannel(window.location.hostname === "sales-network-app.vercel.app" ? "stable" : "beta");
  }, []);
  const isStable = channel === "stable";
  const version = CONFIGURED_VERSION
    ? `${CONFIGURED_VERSION}+${RELEASE_ID}`
    : (isStable ? `1.0.0+${RELEASE_ID}` : `1.0.1-beta.${RELEASE_ID}`);
  return <span className={`release-badge ${isStable ? "release-stable" : "release-beta"}`} title={`Versión ${version}`}><span className="release-dot" aria-hidden="true" />{isStable ? "Producción" : "Beta"} · v{version}</span>;
}
