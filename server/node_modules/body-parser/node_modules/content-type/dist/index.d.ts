/*!
 * content-type
 * Copyright(c) 2015 Douglas Christopher Wilson
 * MIT Licensed
 */
/**
 * The content type object contains a type string and optional parameters.
 */
export interface ContentType {
    type: string;
    parameters: Record<string, string>;
}
/**
 * Format an object into a `Content-Type` header.
 */
export declare function format(obj: Partial<ContentType>): string;
/**
 * Options for parsing a `Content-Type` header.
 */
export interface ParseOptions {
    parameters?: boolean;
}
/**
 * Parse a `Content-Type` header.
 */
export declare function parse(header: string, options?: ParseOptions): ContentType;
