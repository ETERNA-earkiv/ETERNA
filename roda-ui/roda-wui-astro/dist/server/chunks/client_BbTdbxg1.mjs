const API_BASE = "/api/v2";
class ApiError extends Error {
  constructor(status, errorId, message, details) {
    super(message);
    this.status = status;
    this.errorId = errorId;
    this.details = details;
    this.name = "ApiError";
  }
}
async function handleResponse(res) {
  if (!res.ok) {
    let errorId = "UNKNOWN_ERROR";
    let message = `HTTP ${res.status}`;
    let details;
    try {
      const body = await res.json();
      errorId = body.errorId ?? errorId;
      message = body.message ?? message;
      details = body.details;
    } catch {
    }
    throw new ApiError(res.status, errorId, message, details);
  }
  if (res.status === 204) return void 0;
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return res.json();
  }
  return res.text();
}
function buildHeaders(cookie) {
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/json"
  };
  if (cookie) {
    headers["Cookie"] = cookie;
  }
  return headers;
}
async function apiGet(path, opts = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "GET",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    signal: opts.signal
  });
  return handleResponse(res);
}
async function apiPost(path, body, opts = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    body: JSON.stringify(body),
    signal: opts.signal
  });
  return handleResponse(res);
}
async function apiFindRequest(resource, body, opts = {}) {
  return apiPost(`/${resource}/find`, body, opts);
}

export { apiGet as a, apiFindRequest as b };
