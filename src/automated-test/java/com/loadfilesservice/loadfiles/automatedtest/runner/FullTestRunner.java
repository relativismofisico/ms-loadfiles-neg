package com.loadfilesservice.loadfiles.automatedtest.runner;

import com.intuit.karate.junit5.Karate;

/** Ejecuta TODOS los escenarios no marcados como @production-safe exclusivo. */
class FullTestRunner {

    @Karate.Test
    Karate full() {
        return Karate.run("classpath:features")
                .tags("@full,@smoke,@regression,@read-only")
                .relativeTo(getClass());
    }
}