interface JwtPayload {
  exp?: number;
}

export function isJwtExpired(token: string): boolean {
  const payload = decodeJwtPayload(token);
  if (!payload || payload.exp === undefined) {
    return true;
  }
  return payload.exp * 1000 <= Date.now();
}

function decodeJwtPayload(token: string): JwtPayload | null {
  const segments = token.split('.');
  if (segments.length !== 3) {
    return null;
  }
  const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  try {
    return JSON.parse(atob(padded)) as JwtPayload;
  } catch {
    return null;
  }
}
