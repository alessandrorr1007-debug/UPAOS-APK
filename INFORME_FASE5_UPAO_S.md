# UPAO S · Fase 5 · Validación de Pagos

**FASE 5 ESTABILIZADA — VALIDACIONES EN DISPOSITIVO PENDIENTES**

## A. Resumen ejecutivo

El APK debug consultó los siete endpoints institucionales desde Android API 36 con la sesión autorizada existente. Se reforzaron la renovación, la validación del protocolo y el control de publicación de resultados. La aplicación mantuvo el diseño de Pagos y la entrada compartida con Canvas.

## B. Auditoría inicial

Base local `b5630f1`, con cambios previos sin commit y archivos de la Fase 4 sin seguimiento. Se revisó el informe anterior contra esas fuentes locales. Configuración AGP 9.1.1, SDK 37, mínimo 24, OkHttp 4.12, Gson 2.11 y Compose. Las pruebas HTTP usan MockWebServer. La instrumentación usa Compose UI Test y AndroidJUnitRunner.

| Componente | Responsabilidad | Estado inicial | Riesgo | Prueba existente | Prueba adicional |
| --- | --- | --- | --- | --- | --- |
| PaymentService / RealPaymentService | Siete GET, JSON y páginas | Implementado | Tipos coaccionados, cuerpos sin límite, redirección ajena con ruta de login | MockWebServer | Contratos inválidos y límites |
| PaymentDtos | Contrato financiero | BigDecimal y campos opcionales | Nulos dentro de arreglos y campos críticos con tipo incorrecto | Fixtures | Negativos, nulos, espacios |
| PaymentsRepository | Saldo oficial y renovación | Renovación en primer GET | Expiración en detalle u otro GET | Primer GET expirado | Expiración a mitad de carga |
| PaymentsViewModel | Selección y actualización | Cancela trabajos sustituidos | Publicación tardía al invalidar sesión | Cambio rápido y resultado tardío | Invalidación antes de publicar |
| PaymentsScreen / HomeScreen | UI y navegación | Canvas / Pagos en quinto elemento | Recreación y segundo plano | Estados y BackNavigation | Recorrido en dispositivo |
| UpaoAuth | SSO institucional | Reutiliza login académico | Sesión académica vigente con GET financiero expirado | Formulario y rutas | Cierre y nuevo ingreso |
| UpaoHttpClient / AccountSession / UpaoCookieJar | Transporte y cookies aisladas | Invalida y cancela por cuenta | Carreras de invalidación | Cuenta A/B con MockWebServer | Publicación tardía |
| TokenManager / SecureStore | Identidad persistida y credenciales cifradas | Android Keystore | Sesión persistida distinta de la identidad de la consulta | SessionPreferences | Reapertura y cierre |

## C. Autenticación

- **A · Inicio desde cero** — NO VERIFICADO. Se solicitó al propietario que ejecute el ingreso manual en el teléfono.
- **B · Sesión existente** — Verificado en el APK final. La cuenta autorizada cargó el resumen general y dos periodos.
- **C · Expiración** — Verificada con MockWebServer. HTTP 401 y redirección a SSO permiten una renovación por operación. HTTP 403 informa falta de acceso. Un formulario HTML de login se trata como expiración y el HTML genérico se rechaza. Si el segundo intento falla se informa el error.
- **D · Cierre** — La invalidación, cancelación y eliminación del estado se probaron con sesiones sintéticas. El cierre en el teléfono queda NO VERIFICADO hasta confirmar el recorrido manual.
- **E · Cambio de cuenta** — Aislamiento y cancelación verificados con cuentas sintéticas. Alternancia entre cuentas institucionales NO VERIFICADA, al no disponer de una segunda cuenta autorizada para la prueba.

## D. Siete endpoints

Los siete GET se probaron con MockWebServer bajo la ruta base de SSB. El APK final volvió a verificar el recorrido real de resumen y detalle. El proceso no capturó cuerpos, cabeceras de sesión ni importes.

| Ruta relativa | Contrato | Android real |
| --- | --- | --- |
| `accountSummaryOverview/fetchDefaultAccountSummary` | Código interno, offset y página | Resumen general y totales cargados |
| `accountSummary/getBalanceAndConfigInfo` | Código interno, `isOverview=true` | Saldo y monto pendiente mostrados |
| `studentHold/getHoldCount` | Entero no negativo | Consulta completada como parte del resumen |
| `currency/listForUIForStudent` | Código base interno e ISO | Moneda mostrada |
| `accountSummaryByTerm/getMyTermsAndTotals` | `currencyCode=` | Saldo visible en ambos periodos probados |
| `accountSummaryByTerm/getMyTerms` | `loadNext=true`, `max=5`, offset por periodos | Nueve opciones académicas recuperadas |
| `accountSummaryByTerm/fetchAccountSummaryByTerm` | `itemCode` y `termCode` iguales al código seleccionado | Totales en periodo reciente y antiguo |

## E. Precisión financiera

Pruebas sintéticas cubren saldo positivo, cero y negativo, importes decimales, nulos, cargo y pago, ambos en cero, formato PEN, espacios Unicode, códigos con ceros iniciales, totales y páginas incompletas. Los importes oficiales se conservan mediante BigDecimal o texto de presentación. Movimientos idénticos pueden representar cargos independientes. El contrato observado carece de identificador estable para distinguir repeticiones entre páginas.

Comparación paralela con el portal oficial para saldo, importe exigible, saldos por periodo, cargos, pagos y recuento **NO VERIFICADA**. La ejecución en Android confirmó carga y presentación de datos reales. La cantidad exacta de movimientos de todas las páginas reales quedó sin cotejar.

## F. Seguridad

TLS y validación de certificados estándar. El cliente financiero desactiva redirecciones automáticas y caché. La solicitud solo apunta a SSB y las redirecciones externas genéricas se rechazan. Las cookies viven en el almacén de cada cuenta. La renovación usa las credenciales que la app ya guarda cifradas mediante Android Keystore y no añade almacenamiento de secretos. Pagos mantiene resultados en memoria y no escribe respuestas financieras en Logcat. La inspección ZIP del APK confirmó la ausencia del fixture demo. La protección de capturas depende de la configuración de bloqueo biométrico existente.

## G. Estabilidad Android

En API 36 se comprobó abrir Pagos, seleccionar un periodo reciente y uno antiguo, y volver al resumen general. La instalación como actualización conservó la cuenta y la prueba de navegación genérica pasó en instrumentación. La Fase 4 también observó el retorno real de Pagos a Canvas. Cambio de periodo con una carga activa, cancelación y resultado tardío se probaron en JVM. Rotación, recreación explícita, desconexión física durante una solicitud y alternancia real entre cuentas quedan NO VERIFICADAS.

## H. Defectos corregidos

- **Alto · Expiración fuera del primer GET.** Un 401 durante el detalle no renovaba la sesión. Cada operación de resumen o detalle ahora permite un intento de renovación y repite la operación completa. La renovación parte del flujo SSO, incluso si el GET académico todavía acepta la sesión. Regresión con 401 durante el detalle y prueba del límite de intentos.
- **Medio · JSON inválido aceptado.** Gson podía coaccionar tipos de campos de control y admitir nulos en arreglos de filas. El cliente valida estructura, indicadores y enteros, y limita el cuerpo a 2 MiB antes de deserializar. Regresión con formas inválidas y cuerpo sobredimensionado.
- **Medio · Clasificación de redirecciones.** Una ruta de login en un host ajeno disparaba el tratamiento de expiración. Se exige HTTPS y un host institucional de login conocido o una ruta de login del mismo host. Regresión con redirección a un tercero.
- **Medio · Ventana entre respuesta y publicación.** El modelo comprueba la vigencia de la sesión capturada antes de publicar el resumen y el detalle. Regresión con una respuesta que termina al cambiar de cuenta.

## I. Pruebas automatizadas

- JVM e integración simulada: **268 aprobadas**, incluidas 19 de Pagos. Los XML reportan cero fallos, errores y omitidas. La tarea inicial informó `UP-TO-DATE` para la línea base de 265. La compilación final ejecutó la suite con los tres casos adicionales.
- Instrumentación Android: **7 aprobadas** con el APK final, dos de `PaymentsScreenTest` y cinco de `BackNavigationTest`.
- Student Self-Service real: resumen y detalle reciente y antiguo en el APK final. Se documentaron estados observados sin conservar valores personales.

## J. Compilación

`.gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` terminó con `BUILD SUCCESSFUL`. APK local en `app/build/outputs/apk/debug/app-debug.apk`. SDK 37, mínimo 24 y dispositivo API 36. `git diff --check` sin errores. Versión 1.7.9, código 25.

## K. Matriz de aceptación y limitaciones

| Área | Estado |
| --- | --- |
| Autenticación | Sesión existente verificada. Nuevo ingreso pendiente de confirmación |
| Sesiones | Aislamiento sintético aprobado. Dos cuentas reales NO VERIFICADO |
| Siete endpoints | Funcionales en el APK final |
| Precisión financiera | Contratos y formato aprobados. Cotejo independiente con portal NO VERIFICADO |
| Periodos | Reciente, antiguo y todos verificados |
| Paginación | Selector real y páginas simuladas aprobados. Recuento independiente real NO VERIFICADO |
| Errores de red | Pruebas HTTP, timeout y conexión aprobadas |
| Navegación | Entrada desde Canvas verificada. Retorno probado en Fase 4 |
| Pruebas automatizadas | Aprobadas |
| Compilación | Exitosa |
| Seguridad | Sin defectos críticos conocidos en el alcance revisado |

## L. Preparación para Fase 6

La base técnica compila y el flujo principal está operativo. El cotejo independiente con el portal y las pruebas de sesión y ciclo de vida pendientes delimitan la aceptación integral.
