#!/usr/bin/env bash
# ============================================================
# Script de ejecución JMeter — ms-loadfiles-neg
# Uso:
#   ./scripts/run-tests.sh                       # dev, master plan
#   AMBIENTE_PIPE=qa ./scripts/run-tests.sh      # qa, master plan
#   AMBIENTE_PIPE=prod PERFIL=smoke ./scripts/run-tests.sh
#   PLAN=smoke AMBIENTE_PIPE=dev ./scripts/run-tests.sh
# ============================================================
set -euo pipefail

# ── Directorio base (relativo al script) ──
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

# ── Parámetros con defaults ──
AMBIENTE="${AMBIENTE_PIPE:-dev}"
PERFIL="${PERFIL:-master}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

# ── Validar ambiente ──
case "$AMBIENTE" in
  dev|qa|prod) ;;
  *)
    echo "[ERROR] AMBIENTE_PIPE debe ser: dev, qa, prod"
    exit 1
    ;;
esac

# ── Seleccionar plan JMX ──
case "$PERFIL" in
  master)  PLAN_FILE="${BASE_DIR}/plans/ms-loadfiles-master.jmx" ;;
  smoke)   PLAN_FILE="${BASE_DIR}/plans/smoke/ms-loadfiles-smoke.jmx" ;;
  load)    PLAN_FILE="${BASE_DIR}/plans/load/ms-loadfiles-load.jmx" ;;
  stress)  PLAN_FILE="${BASE_DIR}/plans/stress/ms-loadfiles-stress.jmx" ;;
  *)
    echo "[ERROR] PERFIL debe ser: master, smoke, load, stress"
    exit 1
    ;;
esac

PROPS_FILE="${BASE_DIR}/config/${AMBIENTE}.properties"
RESULTS_DIR="${BASE_DIR}/results"
REPORTS_DIR="${BASE_DIR}/reports/run-${AMBIENTE}-${PERFIL}-${TIMESTAMP}"
RESULT_FILE="${RESULTS_DIR}/result-${AMBIENTE}-${PERFIL}-${TIMESTAMP}.jtl"

# ── Validaciones ──
if [ ! -f "$PLAN_FILE" ]; then
  echo "[ERROR] Plan JMX no encontrado: $PLAN_FILE"
  exit 1
fi

if [ ! -f "$PROPS_FILE" ]; then
  echo "[ERROR] Properties no encontrado: $PROPS_FILE"
  exit 1
fi

if ! command -v jmeter &> /dev/null; then
  echo "[ERROR] jmeter no encontrado en PATH. Instalar Apache JMeter 5.6+"
  exit 1
fi

# ── Crear directorios de salida ──
mkdir -p "$RESULTS_DIR" "$REPORTS_DIR"

echo "╔══════════════════════════════════════════════════════╗"
echo "║    ms-loadfiles-neg — JMeter Performance Suite      ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Ambiente : ${AMBIENTE}"
echo "║  Perfil   : ${PERFIL}"
echo "║  Plan     : $(basename "$PLAN_FILE")"
echo "║  Timestamp: ${TIMESTAMP}"
echo "╚══════════════════════════════════════════════════════╝"

# ── Ejecutar JMeter ──
jmeter \
  -n \
  -t "${PLAN_FILE}" \
  -p "${PROPS_FILE}" \
  -JAMBIENTE_PIPE="${AMBIENTE}" \
  -l "${RESULT_FILE}" \
  -e \
  -o "${REPORTS_DIR}" \
  -j "${RESULTS_DIR}/jmeter-${AMBIENTE}-${PERFIL}-${TIMESTAMP}.log"

EXIT_CODE=$?

echo ""
echo "══════════════════════════════════════════════"
if [ $EXIT_CODE -eq 0 ]; then
  echo "✓ Ejecución completada"
else
  echo "✗ Ejecución con errores (exit code: $EXIT_CODE)"
fi
echo "  Resultados : ${RESULT_FILE}"
echo "  Reporte    : ${REPORTS_DIR}/index.html"
echo "══════════════════════════════════════════════"

exit $EXIT_CODE