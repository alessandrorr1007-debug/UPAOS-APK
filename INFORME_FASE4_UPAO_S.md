# UPAO S · Fase 4 · Estado de cuenta

**FASE 4 COMPLETADA — INTEGRACIÓN REAL VERIFICADA**

## A. Resumen ejecutivo

El cliente Android implementa consultas directas a los siete servicios GET de Student Self-Service con la sesión institucional de la cuenta activa. La pantalla muestra saldo total, importe exigible, moneda, fecha de referencia, retenciones y movimientos generales o por periodo. El servicio y los datos de demostración quedaron en las fuentes de prueba.

## B. Arquitectura

- `RealPaymentService` implementa `PaymentService` y maneja transporte, validación JSON y paginación. Las llamadas financieras usan el OkHttp de la cuenta con redirecciones automáticas y caché HTTP desactivadas.
- `PaymentDtos` representa los contratos observados. `PaymentsRepository` distingue los dos saldos, usa los textos del servidor cuando están disponibles y conserva precisión con `BigDecimal`. Normaliza espacios Unicode y conserva los importes ausentes como nulos.
- `PaymentsViewModel` cancela la carga anterior al cambiar de periodo y borra el estado al salir. `PaymentsScreen` presenta estados de carga, vacío y error, con actualización y reintento. `HomeScreen` mantiene el selector Canvas / Pagos en el quinto elemento.

## C. Endpoints

Todas las rutas son relativas a `/StudentSelfService/ssb/`.

| Método | Ruta | Estado del cliente |
| --- | --- | --- |
| GET | `accountSummaryOverview/fetchDefaultAccountSummary` | Implementado, con moneda base y paginación |
| GET | `accountSummary/getBalanceAndConfigInfo` | Implementado, con moneda base e `isOverview=true` |
| GET | `studentHold/getHoldCount` | Implementado |
| GET | `currency/listForUIForStudent` | Implementado, distingue clave interna e ISO |
| GET | `accountSummaryByTerm/getMyTermsAndTotals` | Implementado, con `currencyCode=` |
| GET | `accountSummaryByTerm/getMyTerms` | Implementado, con `currencyCode=`, paginación y deduplicación por clave |
| GET | `accountSummaryByTerm/fetchAccountSummaryByTerm` | Implementado, con `currencyCode=` y paginación |

La opción `allTerm` abre el resumen general. Para los periodos concretos se usa el código seleccionado en `itemCode` y `termCode`, según la captura suministrada. En el selector, `offset` avanza por las filas académicas recibidas y excluye `allTerm`. Los movimientos avanzan por filas recibidas y conservan una sola copia de los totales. El cliente nunca vuelve a pedir el mismo offset dentro de una carga. Las páginas vacías antes del total esperado se tratan como error. Se conservan filas idénticas porque el contrato carece de un ID de movimiento. Si el portal repite filas entre páginas con distintos offsets, el cliente carece de evidencia para distinguirlas de cargos legítimos idénticos.

## D. Autenticación

`UpaoAuth` inicia el flujo académico desde Student Self-Service. Pagos reutiliza el `AccountSession` de ese flujo. Si el primer GET indica expiración y hay credenciales guardadas en `SecureStore` para esa sesión, la app intenta renovar mediante `UpaoAuth` y repite la consulta una vez.

Una consulta anónima a monedas, resumen y selector devolvió HTTP 302 hacia `login.upao.edu.pe/cas/login`. La entrada académica devolvió el mismo destino y la cadena pública llegó al formulario de `upaosso.upao.edu.pe`. La evidencia confirma el punto de entrada SSO. En un teléfono Android con API 36 se instaló el APK debug como actualización. La app conservó la cuenta autorizada y el flujo real mostró saldo, importe exigible, moneda, retenciones y movimientos generales. El selector mostró nueve periodos y, al elegir uno, aparecieron su saldo, totales y movimientos. La actualización manual volvió a cargar el detalle y el selector permitió regresar a Canvas. Como el resumen requiere cinco endpoints exitosos y las dos vistas de movimientos requieren los otros dos, esta ejecución verificó los siete servicios desde Android.

## E. Seguridad

La sesión, el almacén de cookies y la cancelación pertenecen a la cuenta activa. El cierre de sesión y el cambio de cuenta invalidan la sesión, eliminan sus cookies y cancelan las llamadas asociadas. El transporte de Pagos usa TLS estándar y realiza consultas únicamente al host SSB. Pagos conserva resultados en memoria y no registra cuerpos, importes ni credenciales. Los datos de prueba son sintéticos.

## F. Pruebas

- `.gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` terminó con `BUILD SUCCESSFUL`. Pasaron 265 pruebas JVM, incluidas 16 de Pagos. MockWebServer cubrió los siete contratos, consultas GET, expiración, HTML, errores, precisión decimal, páginas múltiples, duplicados legítimos, aislamiento y cancelación.
- `adb shell am instrument -w -e class com.example.upaos.PaymentsScreenTest,com.example.upaos.BackNavigationTest com.example.upaos.test/androidx.test.runner.AndroidJUnitRunner` terminó con `OK (7 tests)` en el teléfono. La instrumentación cubrió carga, error, reintento, saldo cero, estado vacío, selección de periodo y navegación de retorno.
- Validación real en el mismo teléfono con la sesión existente de UPAO S. Se verificó el recorrido Canvas / Pagos, el resumen, la lista de nueve periodos, el detalle de un periodo y su actualización. No se guardaron datos financieros de la sesión.
- `git diff --check` no mostró errores de espacios. La inspección del APK confirmó que el archivo demo quedó fuera del paquete.

## G. Compilación

Compilación debug e instrumentación correctas con SDK 37 y `minSdk 24`. APK debug en `app/build/outputs/apk/debug/app-debug.apk`. El APK de instrumentación está en `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Ambos se instalaron correctamente. Se conservó la versión de producción.

## H. Limitaciones

La ejecución real utilizó la cuenta ya iniciada en el teléfono. Quedan por reproducir el inicio de sesión manual, el cierre de sesión y la alternancia entre dos cuentas institucionales en dispositivo. La invalidación y el aislamiento se probaron con cuentas sintéticas. Se comprobó el regreso a la vista Cursos de Canvas, y las regresiones académicas se cubrieron con la suite JVM. La ejecución real cubrió un periodo concreto, por lo que la correspondencia entre `itemCode` y `termCode` para otros periodos sigue sujeta al contrato del portal.

## I. Próximos pasos

Para ampliar la validación en dispositivo, iniciar sesión desde cero, repetir la consulta, cerrar sesión y volver a abrir Pagos tras iniciar sesión de nuevo. Si el propietario dispone de otra cuenta autorizada, verificar la alternancia entre ambas. Repetir la consulta con varios periodos y comparar los totales con el portal.
