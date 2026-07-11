/**
 * Modrinth API client using native fetch.
 * Base URL: https://api.modrinth.com/v2
 * Auth: Optional PAT via MODRINTH_TOKEN env var.
 * Rate limit: 300 req/min.
 */
const API_BASE = "https://api.modrinth.com/v2";
const USER_AGENT = "sharkengine/modrinth-mcp-server/1.0.0 (github.com/DYAI2025)";
function getHeaders() {
    const headers = {
        "User-Agent": USER_AGENT,
        "Accept": "application/json",
    };
    const token = process.env.MODRINTH_TOKEN;
    if (token) {
        headers["Authorization"] = token;
    }
    return headers;
}
export async function apiGet(path, params) {
    const url = new URL(`${API_BASE}${path}`);
    if (params) {
        for (const [k, v] of Object.entries(params)) {
            if (v !== undefined && v !== "")
                url.searchParams.set(k, v);
        }
    }
    const res = await fetch(url.toString(), { headers: getHeaders() });
    if (!res.ok) {
        const body = await res.text();
        throw new ApiError(res.status, body, path);
    }
    return res.json();
}
export async function apiPatch(path, data) {
    const res = await fetch(`${API_BASE}${path}`, {
        method: "PATCH",
        headers: { ...getHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify(data),
    });
    if (!res.ok) {
        const body = await res.text();
        throw new ApiError(res.status, body, path);
    }
}
export async function apiPostMultipart(path, formData) {
    const headers = getHeaders();
    // Don't set Content-Type — fetch sets it with boundary for FormData
    delete headers["Content-Type"];
    const res = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers,
        body: formData,
    });
    if (!res.ok) {
        const body = await res.text();
        throw new ApiError(res.status, body, path);
    }
    return res.json();
}
export class ApiError extends Error {
    status;
    body;
    path;
    constructor(status, body, path) {
        super(`Modrinth API ${status} on ${path}`);
        this.status = status;
        this.body = body;
        this.path = path;
    }
}
export function handleError(error) {
    if (error instanceof ApiError) {
        switch (error.status) {
            case 401:
                return `Error: Unauthorized. Set MODRINTH_TOKEN env var with a valid PAT.`;
            case 403:
                return `Error: Forbidden. Your token lacks the required scope for ${error.path}.`;
            case 404:
                return `Error: Not found. Check that the project ID/slug is correct.`;
            case 429:
                return `Error: Rate limited (300 req/min). Wait a moment and retry.`;
            default: {
                let detail = "";
                try {
                    const parsed = JSON.parse(error.body);
                    detail = parsed.description || parsed.error || error.body;
                }
                catch {
                    detail = error.body.slice(0, 200);
                }
                return `Error: Modrinth API ${error.status} — ${detail}`;
            }
        }
    }
    return `Error: ${error instanceof Error ? error.message : String(error)}`;
}
//# sourceMappingURL=api.js.map