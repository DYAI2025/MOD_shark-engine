/** Core Modrinth API response types. */

export interface SearchResult {
  hits: SearchHit[];
  offset: number;
  limit: number;
  total_hits: number;
}

export interface SearchHit {
  slug: string;
  title: string;
  description: string;
  categories: string[];
  project_type: string;
  downloads: number;
  follows: number;
  project_id: string;
  author: string;
  versions: string[];
  date_created: string;
  date_modified: string;
  latest_version: string | null;
  license: string;
  client_side: string;
  server_side: string;
  icon_url: string | null;
}

export interface Project {
  id: string;
  slug: string;
  title: string;
  description: string;
  body: string;
  project_type: string;
  status: string;
  categories: string[];
  game_versions: string[];
  loaders: string[];
  client_side: string;
  server_side: string;
  downloads: number;
  followers: number;
  versions: string[];
  license: { id: string; name: string; url: string | null };
  published: string;
  updated: string;
  icon_url: string | null;
  source_url: string | null;
  issues_url: string | null;
  wiki_url: string | null;
  discord_url: string | null;
}

export interface Version {
  id: string;
  project_id: string;
  author_id: string;
  name: string;
  version_number: string;
  changelog: string | null;
  dependencies: VersionDependency[];
  game_versions: string[];
  version_type: string;
  loaders: string[];
  featured: boolean;
  status: string;
  date_published: string;
  downloads: number;
  files: VersionFile[];
}

export interface VersionDependency {
  version_id: string | null;
  project_id: string | null;
  file_name: string | null;
  dependency_type: string;
}

export interface VersionFile {
  hashes: { sha1: string; sha512: string };
  url: string;
  filename: string;
  primary: boolean;
  size: number;
  file_type: string | null;
}

export interface GameVersion {
  version: string;
  version_type: string;
  date: string;
  major: boolean;
}

export interface Loader {
  icon: string;
  name: string;
  supported_project_types: string[];
}

export interface Category {
  icon: string;
  name: string;
  project_type: string;
  header: string;
}

export interface User {
  id: string;
  username: string;
  avatar_url: string | null;
  created: string;
  role: string;
  bio: string | null;
}
