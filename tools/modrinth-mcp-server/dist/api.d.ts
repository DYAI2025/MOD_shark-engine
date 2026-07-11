/**
 * Modrinth API client using native fetch.
 * Base URL: https://api.modrinth.com/v2
 * Auth: Optional PAT via MODRINTH_TOKEN env var.
 * Rate limit: 300 req/min.
 */
export declare function apiGet<T>(path: string, params?: Record<string, string>): Promise<T>;
export declare function apiPatch(path: string, data: unknown): Promise<void>;
export declare function apiPostMultipart<T>(path: string, formData: FormData): Promise<T>;
export declare class ApiError extends Error {
    readonly status: number;
    readonly body: string;
    readonly path: string;
    constructor(status: number, body: string, path: string);
}
export declare function handleError(error: unknown): string;
