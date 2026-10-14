import { parseRequest } from '@/lib/request';
import { json } from '@/lib/response';
import { getAllUserTeams } from '@/queries/prisma';

export async function POST(request: Request) {
  const { auth, error } = await parseRequest(request);

  if (error) {
    return error();
  }

  const teams = await getAllUserTeams(auth.user.id);

  const response = json({ ...auth.user, teams });

  if (auth.token) {
    response.headers.set(
      'Set-Cookie',
      `umami.auth=${encodeURIComponent(auth.token)}; Path=/; Max-Age=315360000; Expires=Fri, 31 Dec 2038 23:59:59 GMT; SameSite=Lax; Secure`,
    );
  }

  return response;
}

