import redis from '@/lib/redis';
import { parseRequest } from '@/lib/request';
import { ok } from '@/lib/response';

export async function POST(request: Request) {
  const { auth, error } = await parseRequest(request);

  if (error) {
    return error();
  }

  if (redis.enabled && auth?.authKey) {
    await redis.client.del(auth.authKey);
  }

  const response = ok();
  response.headers.set(
    'Set-Cookie',
    'umami.auth=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax; Secure',
  );

  return response;
}

