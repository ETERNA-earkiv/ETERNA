import type { Options } from './types.js';
export declare const STATIC_HEADERS_FILE = "_experimentalHeaders.json";
/**
 * Resolves the client directory path at runtime.
 *
 * At build time, we know the relative path between server and client directories.
 * At runtime, we need to find the actual location based on where the server entry is running.
 */
export declare function resolveClientDir(options: Options): string;
