package com.apiautomation.testcase;

/**
 * How to interpret HTTP outcomes for bug detection (not attack testing).
 */
public enum BugOracle {
    /** Happy path — expect success (2xx). */
    ACCEPT,
    /** Invalid input — expect client error (4xx); 2xx/5xx indicate bugs. */
    REJECT,
    /** Soft check — 4xx pass; 2xx is warning (API may intentionally ignore). */
    OBSERVE,
    /** Performance case — verdict is taken from detail (PERF_VERDICT), not HTTP alone. */
    PERF
}
