# UPAO S · Fase 6 · Rediseño de Pagos

## A. Resumen

Rediseño en Jetpack Compose y Material 3 a partir de las referencias clara y oscura. La tarjeta principal destaca el saldo total. El importe pendiente, los cargos y los pagos tienen etiquetas y espacios propios. Los importes largos pueden ocupar más de una línea y las métricas pasan a una columna con poco espacio o escala de texto elevada.

## B. Navegación

El selector de la quinta pestaña conserva Canvas y abre la nueva entrada de Pagos. Estado de cuenta abre Resumen de cuenta. Periodo académico comienza en Todos los periodos y permite abrir un periodo desde el selector o su fila. Atrás devuelve al inicio de Pagos, donde se puede volver a Canvas. Constancias y otros aparece como Próximamente.

## C. Componentes

- `PaymentsScreen.kt` incluye la entrada, el encabezado, las pestañas accesibles, la tarjeta de saldo, las métricas, las filas y los estados de consulta.
- `PaymentsSession.kt` contiene la conexión entre la sesión institucional y el ViewModel.
- `PaymentsViewModel.kt` mantiene el resumen general mientras se consulta un periodo y reutiliza el historial general al volver a Todos los periodos. La salida cancela y limpia el estado.
- `HomeScreen.kt` integra la entrada y el retorno. `Theme.kt` expone el tema activo a través de Compose.
- `PaymentsScreenTest.kt` cubre entrada, pestañas, selector, saldo cero, carga, error, reintento y controles accesibles de actualización y retorno.

## D. Tema claro

Fondo gris muy claro, superficies blancas, bordes suaves y encabezado azul. Los cargos y pagos tienen fondos azul y verde suaves. Los colores se aplican dentro de Pagos.

## E. Tema oscuro

Fondo azul noche, superficies marinas y tarjeta de saldo azul profundo. La pestaña activa usa azul saturado. Ambos temas comparten componentes y se actualizan mediante el selector existente de UPAO S.

## F. Integración financiera

Se conservaron los siete GET, DTO, repositorio, paginación, precisión y aislamiento de la Fase 5. El historial general muestra tres filas inicialmente y permite expandir la lista recuperada. Los movimientos muestran cargo, pago, saldo y fecha cuando están disponibles. La acción de sesión expirada conduce al flujo existente de salida e ingreso.

## G. Pruebas

- JVM e integración local: **268 aprobadas**, sin fallos ni omitidas.
- Android API 36: **8 aprobadas**. Tres pruebas de `PaymentsScreenTest` y cinco de `BackNavigationTest`.
- Teléfono con sesión autorizada: se verificaron la entrada desde el selector, el saldo y el importe pendiente por separado, los totales generales, el listado de periodos, el detalle real de un periodo y el regreso a Canvas. Se alternó al modo claro con el selector de UPAO S y se restauró el tema inicial.

## H. Compilación

`.gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` finalizó con `BUILD SUCCESSFUL`. El APK debug está en `app/build/outputs/apk/debug/app-debug.apk`. Se conservó la versión oficial.

## I. Evidencia visual

Las pruebas Compose usaron datos sintéticos en ambos temas. En el teléfono se comprobó la presencia de los controles y secciones en claro y oscuro mediante la jerarquía de interfaz. La validación con datos reales se registró como estados de pantalla, sin capturas ni importes privados.

## J. Limitaciones

La comprobación en dispositivo se realizó en un solo teléfono. No se verificaron visualmente todos los anchos ni escalas de texto. El cierre y nuevo ingreso desde cero, la alternancia entre dos cuentas reales y el cotejo independiente con el portal conservan el estado pendiente registrado en el informe de la Fase 5.
