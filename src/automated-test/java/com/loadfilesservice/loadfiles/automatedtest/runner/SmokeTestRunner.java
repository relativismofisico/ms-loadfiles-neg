package com.loadfilesservice.loadfiles.automatedtest.runner;

import com.intuit.karate.junit5.Karate;

/** Ejecuta únicamente escenarios @smoke — validación rápida de disponibilidad. */
class SmokeTestRunner {

    @Karate.Test
    Karate smoke() {
        return Karate.run("classpath:features")
                .tags("@smoke")
                .relativeTo(getClass());
    }
}