# Pruebas Funcionales — Karate Framework

Suite de pruebas funcionales/aceptación para el microservicio **ms-loadfiles-neg**.

---

## Estructura de directorios

```
src/automated-test/
├── java/.../automatedtest/
│   ├── runner/
│   │   ├── SmokeTestRunner.java        # @smoke — validación rápida
│   │   ├── RegressionTestRunner.java   # @smoke + @regression — CI estándar
│   │   ├── FullTestRunner.java         # Todos los escenarios no-prod
│   │   └── ProductionSafeRunner.java   # @read-only + @production-safe
│   └── util/
│       └── JwtTestUtil.java            # Generador de JWT para tests
└── resources/
    ├── karate-config.js                # Config multiambiente (carga tokens)
    ├── karate.properties.template      # COPIAR → karate.properties (git-ignorado)
    ├── features/
    │   ├── auth/
    │   │   └── jwt-tokens.feature      # Helper callonce (no ejecutar solo)
    │   ├── loadfiles/
    │   │   ├── list-company-files.feature
    │   │   ├── get-file-by-type.feature
    │   │   ├── upload-company-file.feature
    │   │   └── get-company-file-pdf.feature
    │   ├── filessigned/
    │   │   ├── list-signed-files.feature
    │   │   └── get-signed-file-pdf.feature
    │   ├── filesign/
    │   │   ├── list-files-to-sign.feature
    │   │   ├── get-pdf-to-sign.feature
    │   │   └── upload-signed-pdf.feature
    │   └── signs/
    │       ├── save-sign.feature
    │       ├── get-sign-by-company.feature
    │       └── get-sign-by-user.feature
    ├── schemas/
    │   ├── error-response-schema.json
    │   ├── company-file-response-schema.json
    │   ├── company-file-list-schema.json
    │   ├── file-signed-response-schema.json
    │   ├── file-to-sign-response-schema.json
    │   └── sign-response-schema.json
    └── testfiles/
        ├── dummy.pdf                   # PDF mínimo para pruebas multipart
        └── dummy-sign.png              # PNG mínimo para pruebas de firma
```

---

## Configuración inicial (obligatoria)

### 1. Crear `karate.properties` (git-ignorado)

```bash
cp src/automated-test/resources/karate.properties.template \
   src/automated-test/resources/karate.properties
```

Editar el archivo con los valores del ambiente target:

```properties
# Secreto JWT — mismo valor que security.jwt.secret en application-{env}.yml
karate.jwt.secret=AF84F1FGllNpNnLG055fdg5hGHJK4KGG5VH5TR5J05JFGGDFDGXVV545J4505G666JFGF2mMY95y

# URL base (opcional si es dev local)
karate.base.url=http://localhost:8081/load

# IDs de datos que deben existir en la BD del ambiente
karate.test.companyId=1
karate.test.userId=1
karate.test.fileId=1
karate.test.signedFileId=1
karate.test.fileToSignId=1
karate.test.companyName=empresa-test
karate.test.ipLoad=192.168.1.100
```

> **IMPORTANTE:** `karate.properties` está en `.gitignore`. NUNCA lo subas al repositorio.

---

## Ejecutar pruebas

### Suite smoke (validación rápida — ~2 min)
```bash
./gradlew automatedTest
```

### Suite específica por tag
```bash
# Solo smoke
./gradlew automatedTest -Pkarate.options="--tags @smoke"

# Regression completo
./gradlew automatedTest -Pkarate.options="--tags @regression,@smoke"

# Solo read-only (seguro para producción)
./gradlew automatedTest -Pkarate.options="--tags @read-only,@production-safe"
```

### Contra un ambiente específico

#### Opción A — Línea de comandos (recomendado para CI)
```bash
./gradlew automatedTest \
  -PAMBIENTE_PIPE=qa \
  -Pkarate.jwt.secret="<secret-qa>" \
  -Pkarate.base.url="http://qa-server:8081/load" \
  -Pkarate.test.companyId="5"
```

#### Opción B — Variable de entorno
```bash
AMBIENTE_PIPE=qa ./gradlew automatedTest
```

---

## Tags disponibles

| Tag | Descripción | Seguro en prod |
|-----|-------------|----------------|
| `@smoke` | Escenarios críticos mínimos | Solo GET |
| `@regression` | Suite completa de regresión | No (incluye writes) |
| `@full` | Todos los escenarios | No |
| `@read-only` | Solo escenarios sin efecto en BD | Sí |
| `@production-safe` | Idem read-only, explícitamente marcados | Sí |

### Regla de producción

Si `AMBIENTE_PIPE=prod`, usar `ProductionSafeRunner` que ejecuta únicamente `@read-only` y `@production-safe`. NUNCA ejecutar `FullTestRunner` ni `RegressionTestRunner` apuntando a prod.

---

## Matriz de cobertura

| Endpoint | Auth | 401 | 403 | 200/201 | 404 | 400/422 | Schema |
|----------|------|-----|-----|---------|-----|---------|--------|
| GET /api/listofcompanyfiles/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/companyfile/upload/file/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/companyfile/upload | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| POST /api/companyfilepdf | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| GET /filessigned/listofcompanysignedfiles/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ |
| POST /filessigned/companyfilesignedpdf/{name} | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| GET /filesign/filestosignregistry | ✓ | ✓ | ✓ | ✓ | — | — | ✓ |
| POST /filesign/pdftosign | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| POST /filesign/uploadsignedpdf | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| POST /signs/save | ✓ | ✓ | ✓ | ✓ | — | — | ✓ |
| GET /signs/company/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| GET /signs/user/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |

---

## Reportes

Después de ejecutar `./gradlew automatedTest`, los reportes se generan en:

| Reporte | Ruta |
|---------|------|
| HTML (Karate) | `build/reports/tests/automatedTest/index.html` |
| JUnit XML | `build/test-results/automatedTest/*.xml` |
| Cucumber JSON | `target/surefire-reports/` |

---

## Integración CI/CD

| Sistema | Archivo |
|---------|---------|
| GitHub Actions | `.github/workflows/functional-tests.yml` |
| Jenkins | `Jenkinsfile.functional` |
| GitLab CI | `.gitlab-ci-functional.yml` |

### Secretos requeridos en CI

Configurar en el vault/secrets del sistema CI:

- `KARATE_JWT_SECRET` — secreto JWT del ambiente
- `KARATE_BASE_URL` — URL base del servicio
- `KARATE_TEST_COMPANY_ID` — ID de empresa en datos de prueba
- `KARATE_TEST_USER_ID` — ID de usuario en datos de prueba
- `KARATE_TEST_FILE_ID` — ID de archivo en datos de prueba
- `KARATE_TEST_SIGNED_FILE_ID` — ID de archivo firmado
- `KARATE_TEST_FILE_TO_SIGN_ID` — ID de archivo pendiente de firma
- `KARATE_TEST_COMPANY_NAME` — Nombre de empresa para rutas de archivo
