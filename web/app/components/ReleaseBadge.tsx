"use client";

import { useEffect, useState } from "react";

const CONFIGURED_VERSION = process.env.NEXT_PUBLIC_APP_VERSION || "1.2.0";
const RELEASE_ID = process.env.NEXT_PUBLIC_RELEASE_ID || "local";
const DEPLOYED_CHANNEL = process.env.NEXT_PUBLIC_RELEASE_CHANNEL || "beta";

export default function ReleaseBadge() {
  const [channel, setChannel] = useState(DEPLOYED_CHANNEL);
  useEffect(() => {
    setChannel(DEPLOYED_CHANNEL);
  }, []);
  const isStable = channel === "stable";
  const version = `${CONFIGURED_VERSION}+${RELEASE_ID}`;
  return <span className={`release-badge ${isStable ? "release-stable" : "release-beta"}`} title={`Versión ${version}`}><span className="release-dot" aria-hidden="true" />{isStable ? "Producción" : "Beta"} · v{version}</span>;
}
