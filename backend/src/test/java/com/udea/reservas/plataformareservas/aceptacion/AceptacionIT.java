package com.udea.reservas.plataformareservas.aceptacion;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Ejecuta los escenarios Gherkin de src/test/resources/features contra la API
 * levantada en un puerto aleatorio y una base de datos PostgreSQL en contenedor.
 * La configuración (glue, reportes y etiquetas excluidas) está en junit-platform.properties.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
class AceptacionIT {}
