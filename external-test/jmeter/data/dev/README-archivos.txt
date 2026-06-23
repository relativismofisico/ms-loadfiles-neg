ARCHIVOS DE PRUEBA — REUTILIZADOS DESDE KARATE
=================================================
Los planes JMeter utilizan los mismos archivos de las pruebas funcionales Karate.
NO es necesario crear archivos adicionales.

Archivos utilizados (resueltos desde testfiles.dir en dev.properties):
  src/automated-test/resources/testfiles/dummy.pdf       → E01 upload companyfile
                                                         → E09 upload signed PDF
  src/automated-test/resources/testfiles/dummy-sign.png  → E10 save sign

Propiedad configurada en dev.properties:
  testfiles.dir=src/automated-test/resources/testfiles

Para QA/PROD: actualizar testfiles.dir en qa.properties / prod.properties
apuntando a archivos de prueba del ambiente correspondiente.