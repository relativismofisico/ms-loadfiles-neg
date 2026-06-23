package com.loadfilesservice.loadfiles.automatedtest.runner;

import com.intuit.karate.junit5.Karate;

/**
 * Ejecuta únicamente escenarios @read-only y @production-safe.
 * Nunca dispara POST/PUT/PATCH/DELETE con efecto real en base de datos.
 * Usar en ambiente prod con AMBIENTE_PIPE=prod.
 */
class ProductionSafeRunner {

    @Karate.Test
    Karate productionSafe() {
        return Karate.run("classpath:features")
                .tags("@read-only,@production-safe")
                .relativeTo(getClass());
    }
}