package org.ruyisdk.ruyi.model;

/**
 * Telemetry mode enumeration.
 */
public enum TelemetryMode {
    /** Fully enabled — send anonymous usage data. */
    ON,
    /** Local analysis only — no data sent. */
    LOCAL,
    /** Completely disabled — no data collection or analysis. */
    OFF
}
