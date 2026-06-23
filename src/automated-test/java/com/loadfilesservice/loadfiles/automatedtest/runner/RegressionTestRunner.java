package com.loadfilesservice.loadfiles.automatedtest.runner;

import com.intuit.karate.junit5.Karate;

/** Ejecuta escenarios @smoke y @regression — suite estándar de CI. */
class RegressionTestRunner {

    @Karate.Test
    Karate regression() {
        return Karate.run("classpath:features")
                .tags("@regression,@smoke")
                .relativeTo(getClass());
    }
}