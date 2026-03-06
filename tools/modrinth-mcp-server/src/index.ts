#!/usr/bin/env node
/**
 * Modrinth MCP Server
 *
 * Provides tools to search, browse, and publish Minecraft mods via the Modrinth API.
 * Auth: Set MODRINTH_TOKEN env var for write operations (create/modify).
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { apiGet, apiPatch, apiPostMultipart, handleError } from "./api.js";
import type {
  SearchResult, Project, Version, GameVersion, Loader, Category, User,
} from "./types.js";

const server = new McpServer({
  name: "modrinth-mcp-server",
  version: "1.0.0",
});

// ──────────────────────────────────────────────
// Tool: modrinth_search_projects
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_search_projects",
  {
    title: "Search Modrinth Projects",
    description:
      `Search for mods, modpacks, resource packs, or shaders on Modrinth.

Supports faceted filtering by categories, game versions, loaders, and project type.
Facets use AND between groups, OR within a group: [["categories:fabric"],["versions:1.21.1"]]

Args:
  - query: Search text
  - facets: Optional JSON facet string (e.g. [["categories:fabric"],["versions:1.21.1"]])
  - index: Sort order — relevance, downloads, follows, newest, updated
  - limit: Results per page (1-100, default 10)
  - offset: Pagination offset

Returns: Array of project summaries with download counts and metadata.`,
    inputSchema: {
      query: z.string().describe("Search text"),
      facets: z.string().optional().describe('Facet filter JSON, e.g. [["categories:fabric"],["versions:1.21.1"]]'),
      index: z.enum(["relevance", "downloads", "follows", "newest", "updated"]).default("relevance").describe("Sort order"),
      limit: z.number().int().min(1).max(100).default(10).describe("Results per page"),
      offset: z.number().int().min(0).default(0).describe("Pagination offset"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ query, facets, index, limit, offset }) => {
    try {
      const params: Record<string, string> = {
        query,
        index,
        limit: String(limit),
        offset: String(offset),
      };
      if (facets) params.facets = facets;

      const data = await apiGet<SearchResult>("/search", params);

      if (data.total_hits === 0) {
        return { content: [{ type: "text", text: `No projects found for "${query}".` }] };
      }

      const lines = [`# Search: "${query}" (${data.total_hits} results)`, ""];
      for (const hit of data.hits) {
        lines.push(`## ${hit.title} (${hit.slug})`);
        lines.push(`- **Type**: ${hit.project_type} | **Downloads**: ${hit.downloads.toLocaleString()} | **Author**: ${hit.author}`);
        lines.push(`- **ID**: ${hit.project_id}`);
        lines.push(`- ${hit.description}`);
        lines.push("");
      }
      if (data.total_hits > offset + data.hits.length) {
        lines.push(`_Showing ${offset + 1}–${offset + data.hits.length} of ${data.total_hits}. Use offset=${offset + data.hits.length} for next page._`);
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_get_project
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_get_project",
  {
    title: "Get Modrinth Project",
    description:
      `Get full details of a Modrinth project by ID or slug.

Returns: title, description, body (markdown), categories, game versions, loaders, download/follower counts, links, license.`,
    inputSchema: {
      id_or_slug: z.string().describe("Project ID (base62) or slug"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ id_or_slug }) => {
    try {
      const p = await apiGet<Project>(`/project/${encodeURIComponent(id_or_slug)}`);

      const lines = [
        `# ${p.title} (${p.slug})`,
        "",
        `**Type**: ${p.project_type} | **Status**: ${p.status} | **License**: ${p.license.id}`,
        `**Downloads**: ${p.downloads.toLocaleString()} | **Followers**: ${p.followers.toLocaleString()}`,
        `**Categories**: ${p.categories.join(", ")}`,
        `**Loaders**: ${p.loaders.join(", ")}`,
        `**Game Versions**: ${p.game_versions.join(", ")}`,
        `**Client**: ${p.client_side} | **Server**: ${p.server_side}`,
        "",
        `> ${p.description}`,
        "",
        `**ID**: ${p.id}`,
        `**Published**: ${p.published} | **Updated**: ${p.updated}`,
        `**Versions**: ${p.versions.length} total`,
      ];
      if (p.source_url) lines.push(`**Source**: ${p.source_url}`);
      if (p.issues_url) lines.push(`**Issues**: ${p.issues_url}`);
      if (p.discord_url) lines.push(`**Discord**: ${p.discord_url}`);

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_list_versions
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_list_versions",
  {
    title: "List Project Versions",
    description:
      `List all versions of a Modrinth project, with optional filters for loader and game version.

Returns: version name, number, type (release/beta/alpha), game versions, loaders, download count, file URLs.`,
    inputSchema: {
      id_or_slug: z.string().describe("Project ID or slug"),
      loaders: z.string().optional().describe('JSON array of loaders, e.g. ["fabric"]'),
      game_versions: z.string().optional().describe('JSON array of MC versions, e.g. ["1.21.1"]'),
      featured: z.boolean().optional().describe("Filter by featured status"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ id_or_slug, loaders, game_versions, featured }) => {
    try {
      const params: Record<string, string> = {};
      if (loaders) params.loaders = loaders;
      if (game_versions) params.game_versions = game_versions;
      if (featured !== undefined) params.featured = String(featured);

      const versions = await apiGet<Version[]>(`/project/${encodeURIComponent(id_or_slug)}/version`, params);

      if (versions.length === 0) {
        return { content: [{ type: "text", text: "No versions found matching filters." }] };
      }

      const lines = [`# Versions of ${id_or_slug} (${versions.length} total)`, ""];
      for (const v of versions) {
        const primary = v.files.find(f => f.primary) ?? v.files[0];
        lines.push(`## ${v.name} (${v.version_number})`);
        lines.push(`- **Type**: ${v.version_type} | **Downloads**: ${v.downloads.toLocaleString()} | **Status**: ${v.status}`);
        lines.push(`- **Game versions**: ${v.game_versions.join(", ")} | **Loaders**: ${v.loaders.join(", ")}`);
        lines.push(`- **ID**: ${v.id} | **Published**: ${v.date_published}`);
        if (primary) {
          lines.push(`- **File**: ${primary.filename} (${(primary.size / 1024).toFixed(0)} KB)`);
          lines.push(`- **URL**: ${primary.url}`);
        }
        if (v.dependencies.length > 0) {
          const deps = v.dependencies.map(d => `${d.project_id ?? d.file_name} (${d.dependency_type})`);
          lines.push(`- **Dependencies**: ${deps.join(", ")}`);
        }
        lines.push("");
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_get_version
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_get_version",
  {
    title: "Get Version Details",
    description:
      `Get full details of a specific version by its ID. Includes changelog, files with hashes, and dependencies.`,
    inputSchema: {
      version_id: z.string().describe("Version ID (base62)"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ version_id }) => {
    try {
      const v = await apiGet<Version>(`/version/${encodeURIComponent(version_id)}`);

      const lines = [
        `# ${v.name} (${v.version_number})`,
        "",
        `**Project**: ${v.project_id} | **Type**: ${v.version_type} | **Status**: ${v.status}`,
        `**Downloads**: ${v.downloads.toLocaleString()} | **Published**: ${v.date_published}`,
        `**Game versions**: ${v.game_versions.join(", ")}`,
        `**Loaders**: ${v.loaders.join(", ")}`,
        "",
      ];

      if (v.files.length > 0) {
        lines.push("## Files");
        for (const f of v.files) {
          lines.push(`- **${f.filename}** (${(f.size / 1024).toFixed(0)} KB)${f.primary ? " [PRIMARY]" : ""}`);
          lines.push(`  SHA1: ${f.hashes.sha1}`);
          lines.push(`  URL: ${f.url}`);
        }
        lines.push("");
      }

      if (v.dependencies.length > 0) {
        lines.push("## Dependencies");
        for (const d of v.dependencies) {
          lines.push(`- ${d.project_id ?? d.file_name ?? "unknown"} (${d.dependency_type})`);
        }
        lines.push("");
      }

      if (v.changelog) {
        lines.push("## Changelog", "", v.changelog);
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_create_version
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_create_version",
  {
    title: "Create Version (Upload)",
    description:
      `Upload a new version to a Modrinth project. Requires MODRINTH_TOKEN with VERSION_CREATE scope.

Reads a local JAR file and uploads it as a new version. The file_path must be an absolute path to the .jar file.

Args:
  - project_id: The project ID or slug to publish to
  - version_number: Semver string (e.g. "0.0.2")
  - name: Display name (e.g. "Shark Engine v0.0.2")
  - game_versions: JSON array of MC versions (e.g. ["1.21.1"])
  - loaders: JSON array of loaders (e.g. ["fabric"])
  - version_type: release, beta, or alpha
  - changelog: Markdown changelog text
  - file_path: Absolute path to the .jar file to upload
  - dependencies: Optional JSON array of {project_id, dependency_type} objects`,
    inputSchema: {
      project_id: z.string().describe("Project ID or slug"),
      version_number: z.string().describe('Semver version, e.g. "0.0.2"'),
      name: z.string().describe('Display name, e.g. "Shark Engine v0.0.2"'),
      game_versions: z.string().describe('JSON array, e.g. ["1.21.1"]'),
      loaders: z.string().describe('JSON array, e.g. ["fabric"]'),
      version_type: z.enum(["release", "beta", "alpha"]).default("beta").describe("Release channel"),
      changelog: z.string().default("").describe("Markdown changelog"),
      file_path: z.string().describe("Absolute path to the .jar file"),
      dependencies: z.string().optional().describe('Optional JSON array of {project_id, dependency_type} objects'),
    },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: true },
  },
  async ({ project_id, version_number, name, game_versions, loaders, version_type, changelog, file_path, dependencies }) => {
    try {
      if (!process.env.MODRINTH_TOKEN) {
        return {
          content: [{ type: "text", text: "Error: MODRINTH_TOKEN env var is required for uploads. Create a PAT at modrinth.com/settings/pats with VERSION_CREATE scope." }],
          isError: true,
        };
      }

      const absPath = resolve(file_path);
      const fileBuffer = await readFile(absPath);
      const fileName = absPath.split("/").pop() ?? "mod.jar";

      const versionData = {
        name,
        version_number,
        changelog,
        dependencies: dependencies ? JSON.parse(dependencies) : [],
        game_versions: JSON.parse(game_versions),
        version_type,
        loaders: JSON.parse(loaders),
        featured: false,
        status: "listed",
        project_id,
        file_parts: ["file"],
        primary_file: "file",
      };

      const form = new FormData();
      form.append("data", JSON.stringify(versionData));
      form.append("file", new Blob([fileBuffer]), fileName);

      const version = await apiPostMultipart<Version>("/version", form);

      const primary = version.files.find(f => f.primary) ?? version.files[0];
      return {
        content: [{
          type: "text",
          text: [
            `Version created successfully!`,
            "",
            `**${version.name}** (${version.version_number})`,
            `**ID**: ${version.id}`,
            `**Status**: ${version.status}`,
            `**File**: ${primary?.filename ?? "unknown"} (${primary ? (primary.size / 1024).toFixed(0) + " KB" : ""})`,
            `**URL**: https://modrinth.com/mod/${project_id}/version/${version.version_number}`,
          ].join("\n"),
        }],
      };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_modify_project
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_modify_project",
  {
    title: "Modify Project",
    description:
      `Update project settings on Modrinth. Requires MODRINTH_TOKEN with PROJECT_WRITE scope.
Only the provided fields will be updated (partial update).

Args:
  - id_or_slug: Project ID or slug
  - fields_json: JSON object with fields to update. Supported: title, description, body, categories, status, client_side, server_side, issues_url, source_url, wiki_url, discord_url`,
    inputSchema: {
      id_or_slug: z.string().describe("Project ID or slug"),
      fields_json: z.string().describe("JSON object with fields to update"),
    },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ id_or_slug, fields_json }) => {
    try {
      if (!process.env.MODRINTH_TOKEN) {
        return {
          content: [{ type: "text", text: "Error: MODRINTH_TOKEN env var required. Create a PAT at modrinth.com/settings/pats." }],
          isError: true,
        };
      }
      const fields = JSON.parse(fields_json);
      await apiPatch(`/project/${encodeURIComponent(id_or_slug)}`, fields);
      return {
        content: [{ type: "text", text: `Project ${id_or_slug} updated successfully. Fields changed: ${Object.keys(fields).join(", ")}` }],
      };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_list_game_versions
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_list_game_versions",
  {
    title: "List Game Versions",
    description: `List available Minecraft game versions on Modrinth. Optionally filter by type (release/snapshot) or major releases only.`,
    inputSchema: {
      type_filter: z.enum(["release", "snapshot", "all"]).default("release").describe("Version type filter"),
      major_only: z.boolean().default(true).describe("Only show major releases"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ type_filter, major_only }) => {
    try {
      let versions = await apiGet<GameVersion[]>("/tag/game_version");

      if (type_filter !== "all") {
        versions = versions.filter(v => v.version_type === type_filter);
      }
      if (major_only) {
        versions = versions.filter(v => v.major);
      }

      const lines = [`# Minecraft Game Versions (${versions.length})`, ""];
      for (const v of versions.slice(0, 50)) {
        lines.push(`- **${v.version}** (${v.version_type})${v.major ? " [MAJOR]" : ""} — ${v.date}`);
      }
      if (versions.length > 50) {
        lines.push(`\n_...and ${versions.length - 50} more. Use type_filter/major_only to narrow._`);
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_list_loaders
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_list_loaders",
  {
    title: "List Mod Loaders",
    description: `List all available mod loaders on Modrinth (fabric, forge, quilt, neoforge, etc.) with their supported project types.`,
    inputSchema: {},
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async () => {
    try {
      const loaders = await apiGet<Loader[]>("/tag/loader");
      const lines = [`# Available Loaders (${loaders.length})`, ""];
      for (const l of loaders) {
        lines.push(`- **${l.name}** — supports: ${l.supported_project_types.join(", ")}`);
      }
      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_list_categories
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_list_categories",
  {
    title: "List Categories",
    description: `List all available project categories on Modrinth, optionally filtered by project type.`,
    inputSchema: {
      project_type: z.enum(["mod", "modpack", "resourcepack", "shader", "all"]).default("mod").describe("Filter by project type"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ project_type }) => {
    try {
      let categories = await apiGet<Category[]>("/tag/category");
      if (project_type !== "all") {
        categories = categories.filter(c => c.project_type === project_type);
      }

      const byHeader = new Map<string, Category[]>();
      for (const c of categories) {
        const list = byHeader.get(c.header) ?? [];
        list.push(c);
        byHeader.set(c.header, list);
      }

      const lines = [`# Categories for ${project_type} (${categories.length})`, ""];
      for (const [header, cats] of byHeader) {
        lines.push(`## ${header}`);
        for (const c of cats) {
          lines.push(`- ${c.name}`);
        }
        lines.push("");
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Tool: modrinth_get_user_projects
// ──────────────────────────────────────────────

server.registerTool(
  "modrinth_get_user_projects",
  {
    title: "Get User Projects",
    description: `List all projects owned by a Modrinth user. Pass a username or user ID.`,
    inputSchema: {
      user: z.string().describe("Username or user ID"),
    },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
  },
  async ({ user }) => {
    try {
      const projects = await apiGet<Project[]>(`/user/${encodeURIComponent(user)}/projects`);

      if (projects.length === 0) {
        return { content: [{ type: "text", text: `No projects found for user "${user}".` }] };
      }

      const lines = [`# Projects by ${user} (${projects.length})`, ""];
      for (const p of projects) {
        lines.push(`## ${p.title} (${p.slug})`);
        lines.push(`- **Type**: ${p.project_type} | **Status**: ${p.status} | **Downloads**: ${p.downloads.toLocaleString()}`);
        lines.push(`- **ID**: ${p.id}`);
        lines.push(`- ${p.description}`);
        lines.push("");
      }

      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e) {
      return { content: [{ type: "text", text: handleError(e) }], isError: true };
    }
  },
);

// ──────────────────────────────────────────────
// Start server
// ──────────────────────────────────────────────

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Modrinth MCP server running via stdio");
}

main().catch((err) => {
  console.error("Fatal:", err);
  process.exit(1);
});
