/**
 * The parent login.
 *
 * Not the legacy /authenticate-student: that authenticates a username alone and
 * puts it in the token, which cannot identify a child now that two students may
 * share one. This endpoint authenticates the (username, password) pair — the
 * combination the school's rule makes unique — and returns a token keyed by the
 * student's id.
 */
export const LOGIN_ENDPOINT = '/api/parent/authenticate';