package com.apiautomation.testcase;

import java.util.List;

/**
 * Catalog of functional bug-detection cases (no security attack packs).
 */
public final class BugTestCatalog {

    private BugTestCatalog() {
    }

    public static List<BugTestCaseDef> all() {
        return List.of(
                new BugTestCaseDef(
                        "smoke",
                        "Basic Smoke Test",
                        "P0",
                        "Identify if the API handles a valid request successfully (happy path).",
                        "HTTP 2xx (Success)",
                        BugOracle.ACCEPT
                ),
                new BugTestCaseDef(
                        "invalid_payload",
                        "Invalid Payload Body",
                        "P1",
                        "Identify if the API mishandles type-invalid field values (validation bug).",
                        "HTTP 400 or 422 (reject invalid types); not 2xx or 5xx",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "missing_headers",
                        "Missing Headers",
                        "P1",
                        "Identify if the API accepts requests when headers are removed (contract bug).",
                        "HTTP 4xx (reject); HTTP 2xx indicates missing header validation",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "missing_auth",
                        "Missing Authentication",
                        "P0",
                        "Identify if the API allows access without credentials when auth was present (auth gate bug).",
                        "HTTP 401 or 403",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "wrong_method",
                        "Unsupported HTTP Method",
                        "P1",
                        "Identify if the API mishandles an unsupported HTTP method (routing/method bug).",
                        "HTTP 405, 404, or 501; not 5xx",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "missing_body",
                        "Missing Request Body",
                        "P1",
                        "Identify if the API accepts a body-required request with no body (validation bug).",
                        "HTTP 400, 415, or 422",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "invalid_path",
                        "Invalid Path Parameters",
                        "P1",
                        "Identify if the API returns a clear error for an invalid path id (resource lookup bug).",
                        "HTTP 404 or 400",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "invalid_query",
                        "Invalid Query Parameters",
                        "P2",
                        "Identify if invalid query values cause server errors or unclear behaviour.",
                        "HTTP 4xx preferred; HTTP 2xx may mean params are optional (warning); not 5xx",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "missing_query",
                        "Missing Query Parameters",
                        "P2",
                        "Identify if removing query params breaks required-query contracts.",
                        "HTTP 4xx if required; HTTP 2xx if optional (warning); not 5xx",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "wrong_content_type",
                        "Wrong Content-Type",
                        "P1",
                        "Identify if the API mishandles an incorrect Content-Type (media-type handling bug).",
                        "HTTP 400, 415, or 422 preferred; 2xx may be lenient (warning); not 5xx",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "malformed_json",
                        "Malformed JSON",
                        "P1",
                        "Identify if the API accepts or crashes on invalid JSON (parsing/validation bug).",
                        "HTTP 400 or 422; not 2xx or 5xx",
                        BugOracle.REJECT
                ),
                new BugTestCaseDef(
                        "boundary_values",
                        "Boundary / Oversized Payload",
                        "P2",
                        "Identify if a very large body causes server errors (stability / size-handling bug).",
                        "HTTP 413/400/422 or controlled reject; HTTP 5xx indicates a bug; 2xx may be warning",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "special_characters",
                        "Special Characters",
                        "P2",
                        "Identify if special/Unicode characters cause crashes or unexpected 5xx (encoding bug).",
                        "HTTP 2xx or 4xx; HTTP 5xx indicates a bug",
                        BugOracle.OBSERVE
                ),
                new BugTestCaseDef(
                        "empty_values",
                        "Empty Values",
                        "P2",
                        "Identify if empty/blank field values are accepted when they should be rejected (validation bug).",
                        "HTTP 400 or 422 preferred; 2xx may mean empty allowed (warning); not 5xx",
                        BugOracle.OBSERVE
                )
        );
    }
}
