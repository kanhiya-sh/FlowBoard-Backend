package com.flowboard.gateway.config;

// Routes are defined in application.properties using lb:// URIs (Eureka service discovery).
// This class previously held hardcoded localhost routes that conflicted with the properties-based
// routes and caused downstream CORS headers (with wrong origin) to leak through the gateway.
// Intentionally left empty — kept as a placeholder so the package is not deleted.
