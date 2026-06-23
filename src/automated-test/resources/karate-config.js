function fn() {
    // ── Ambiente ─────────────────────────────────────────────────────────────
    var env = karate.env || 'dev';
    karate.log('karate.env =', env);

    // ── Configuración base por ambiente ───────────────────────────────────────
    var config = {
        env: env,
        isProd: env === 'prod',

        // Valores sobreescribibles en karate.properties o -Pkarate.xxx
        baseUrl:    karate.properties['karate.base.url'],
        jwtSecret:  karate.properties['karate.jwt.secret'],

        // IDs de datos de prueba (configurables por ambiente)
        testCompanyId:     karate.properties['karate.test.companyId']     || '1',
        testUserId:        karate.properties['karate.test.userId']         || '1',
        testFileId:        karate.properties['karate.test.fileId']         || '1',
        testSignedFileId:  karate.properties['karate.test.signedFileId']   || '1',
        testFileToSignId:  karate.properties['karate.test.fileToSignId']   || '1',
        testCompanyName:   karate.properties['karate.test.companyName']    || 'empresa-test',
        testIpLoad:        karate.properties['karate.test.ipLoad']         || '192.168.1.100',
    };

    if (env === 'dev') {
        config.baseUrl = config.baseUrl || 'http://localhost:8081/load';
    } else if (env === 'qa') {
        config.baseUrl = config.baseUrl || 'http://qa-server:8081/load';
    } else if (env === 'staging') {
        config.baseUrl = config.baseUrl || 'http://staging-server:8081/load';
    } else if (env === 'prod') {
        config.baseUrl = config.baseUrl || 'http://prod-server:8081/load';
    }

    // ── Generación de JWT (callonce — un token por rol por suite) ─────────────
    if (!config.jwtSecret) {
        karate.log('ADVERTENCIA: karate.jwt.secret no configurado — los tests de auth fallarán.');
    }

    var JwtUtil = Java.type('com.loadfilesservice.loadfiles.automatedtest.util.JwtTestUtil');

    // Tokens válidos por rol
    config.tokenAdmin      = JwtUtil.generateToken('test-admin',     'ADMINISTRADOR');
    config.tokenEmpresa    = JwtUtil.generateToken('test-empresa',   'EMPRESA');
    config.tokenOperario   = JwtUtil.generateToken('test-operario',  'OPERARIO');
    config.tokenFondeador  = JwtUtil.generateToken('test-fondeador', 'FONDEADOR');

    // Token sin rol autorizado (rol inventado)
    config.tokenSinAcceso  = JwtUtil.generateToken('test-noaccess',  'ROL_INEXISTENTE');

    // Tokens negativos
    config.tokenExpirado   = JwtUtil.generateExpiredToken('test-admin', 'ADMINISTRADOR');
    config.tokenFirmaInvalida = JwtUtil.generateInvalidSignatureToken('test-admin', 'ADMINISTRADOR');

    // ── Configuración HTTP global ─────────────────────────────────────────────
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 30000);
    karate.configure('ssl', true);
    // No se define configure('headers') global — cada feature gestiona su propia auth
    // usando "* configure headers = { Authorization: 'Bearer ' + tokenXxx }" en Background

    return config;
}