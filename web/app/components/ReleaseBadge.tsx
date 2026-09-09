"use client";

import { useEffect, useState } from "react";

const CONFIGURED_VERSION = process.env.NEXT_PUBLIC_APP_VERSION;

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
  const version = CONFIGURED_VERSION || (isStable ? "1.0.0" : "1.0.1-beta.1");
  return <span className={`release-badge ${isStable ? "release-stable" : "release-beta"}`} title={`Versión ${version}`}><span className="release-dot" aria-hidden="true" />{isStable ? "Producción" : "Beta"} · v{version}</span>;
}
