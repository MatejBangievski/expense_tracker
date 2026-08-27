import { isJwtExpired } from './token';

function tokenWithExp(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ exp: expSeconds }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${payload}.signature`;
}

describe('isJwtExpired', () => {
  it('returns false for a token expiring in the future', () => {
    const future = Math.floor(Date.now() / 1000) + 60;
    expect(isJwtExpired(tokenWithExp(future))).toBe(false);
  });

  it('returns true for a token that has already expired', () => {
    const past = Math.floor(Date.now() / 1000) - 60;
    expect(isJwtExpired(tokenWithExp(past))).toBe(true);
  });

  it('returns true when the exp claim is missing', () => {
    const payload = btoa(JSON.stringify({ sub: 'user@example.com' }));
    expect(isJwtExpired(`header.${payload}.signature`)).toBe(true);
  });

  it('returns true for a malformed token', () => {
    expect(isJwtExpired('not-a-jwt')).toBe(true);
  });
});
