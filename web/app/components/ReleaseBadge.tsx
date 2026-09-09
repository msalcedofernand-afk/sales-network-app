"use client";

import { useEffect, useState } from "react";

const VERSION = process.env.NEXT_PUBLIC_APP_VERSION || "1.0.0";

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
  return <span className={`release-badge ${isStable ? "release-stable" : "release-beta"}`} title={`Versión ${VERSION}`}><span className="release-dot" aria-hidden="true" />{isStable ? "Producción" : "Beta"} · v{VERSION}</span>;
}
