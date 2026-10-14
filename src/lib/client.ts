import { getItem, removeItem, setItem } from '@/lib/storage';
import { AUTH_TOKEN } from './constants';

const TEN_YEARS_SEC = 315360000;

export function getClientAuthToken() {
  const token = getItem(AUTH_TOKEN);
  if (token && token !== 'null' && token !== 'undefined') {
    return token;
  }

  if (typeof document !== 'undefined') {
    const match = document.cookie.match(/(?:^|;\s*)umami\.auth=([^;]+)/);
    if (match) {
      const cookieToken = decodeURIComponent(match[1]);
      if (cookieToken && cookieToken !== 'null' && cookieToken !== 'undefined') {
        try {
          setItem(AUTH_TOKEN, cookieToken);
        } catch {}
        return cookieToken;
      }
    }
  }

  return null;
}

export function setClientAuthToken(token: string) {
  setItem(AUTH_TOKEN, token);
  if (typeof document !== 'undefined' && token) {
    document.cookie = `${AUTH_TOKEN}=${encodeURIComponent(token)}; path=/; max-age=${TEN_YEARS_SEC}; expires=Fri, 31 Dec 2038 23:59:59 GMT; SameSite=Lax; Secure`;
  }
}

export function removeClientAuthToken() {
  removeItem(AUTH_TOKEN);
  if (typeof document !== 'undefined') {
    document.cookie = `${AUTH_TOKEN}=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax; Secure`;
  }
}

