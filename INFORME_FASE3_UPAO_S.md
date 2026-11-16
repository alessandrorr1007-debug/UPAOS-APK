# Fase 3 de UPAO S

## 1. Resultado

La quinta posición de la barra inferior ofrece un selector Material 3 con **Canvas** y **Pagos**. Pagos abre el Estado de cuenta existente con el aviso de demostración. El transporte financiero institucional sigue pendiente de capturas autenticadas y de una verificación en Android.

## 2. Selector Canvas/Pagos

La quinta posición empieza como Canvas y abre el menú al tocar la flecha, el icono o la etiqueta. Elegir Canvas abre la página existente. Elegir Pagos muestra Estado de cuenta dentro de la misma posición y mantiene visible la barra inferior. El icono y la etiqueta reflejan la selección. Atrás desde Pagos vuelve a la página desde la que se abrió y restablece Canvas como opción del selector. El acceso del menú lateral usa la misma pantalla y el enlace de Canvas desde Hoy abre Canvas directamente. Al alternar entre ambos módulos, se recompone la vista de Canvas y se pueden reiniciar sus selecciones internas en memoria.

## 3. Archivos

- `MainActivity.kt` retira el destino separado de pagos y mantiene la pantalla principal como punto de entrada.
- `ui/home/HomeScreen.kt` añade el menú a la quinta posición y controla la selección y el retorno.
- `ui/payments/PaymentsScreen.kt` integra el contenido y la barra de Estado de cuenta en la pantalla principal. También muestra saldos disponibles en el selector de periodos.
- `data/payments/PaymentsRepository.kt` distingue un pendiente numérico cero y visible de un importe ausente u oculto.
- `app/src/test/java/com/example/upaos/PaymentsTest.kt` verifica esa distinción, además de conservar las pruebas anteriores.
- `INFORME_FASE3_UPAO_S.md` registra implementación, evidencias y verificación.

Las rutas Kotlin relativas de producción parten de `app/src/main/java/com/example/upaos/`.

## 4. Arquitectura y autenticación

`HomeScreen → PaymentsScreen / PaymentsTopBar → PaymentsViewModel → PaymentsRepository → PaymentService → DemoPaymentService`. El ViewModel se vincula a la entrada principal y a la generación de la sesión local. Al salir de la vista se cancela su consulta y se borra el estado de pagos. El proveedor de demostración lee el JSON sintético empaquetado. El futuro transporte se conectaría desde Android a `https://ssb.upao.edu.pe` mediante una implementación de `PaymentService`.

`UpaoAuth` abre la sesión para notas de Banner a través del SSO. La observación pública de la Fase 2 llegó al formulario institucional pasando por CAS y `commonauth`. Ese hallazgo describe el acceso anónimo a notas. La autenticación real de pagos sigue por verificar. El cliente de notas ya aísla cookies y peticiones por estudiante mediante `AccountSession` y `UpaoCookieJar`, y `TokenManager` protege el estado persistente mediante `SecureStore`. El cambio de cuenta invalida la sesión anterior. Esta fase usa la identidad local solo para el alcance del ViewModel y no intenta iniciar una segunda sesión para pagos.

## 5. Estado de los servicios financieros

Rutas bajo `https://ssb.upao.edu.pe/StudentSelfService/ssb/`. Los adjuntos, los informes y el código local carecen de capturas que confirmen métodos, codificación o reutilización de sesión. La documentación pública de Ellucian describe la función Student Account a nivel de producto. [Descripción oficial de Banner Self-Service](https://www.ellucian.com/assets/en/brochure/brochure-learn-more-about-ellucian-banner-self-service.pdf)

| Ruta | Método HTTP | Estado |
| --- | --- | --- |
| `accountSummary/getBalanceAndConfigInfo` | Desconocido | Simulado. Saldo, pendiente, moneda y fecha. |
| `accountSummaryByTerm/getMyTermsAndTotals` | Desconocido | Simulado. Totales y saldos por periodo. |
| `accountSummaryByTerm/fetchAccountSummaryByTerm` | Desconocido | Simulado. Movimientos y totales por periodo. Paginación pendiente. |
| `studentHold/getHoldCount` | Desconocido | Simulado. Conteo de retenciones. |
| `accountSummaryByTerm/getMyTerms` | Desconocido | Simulado. Selector y códigos de periodo. Paginación pendiente. |
| `currency/listForUIForStudent` | Desconocido | Simulado. Moneda y configuración. |

El módulo también conserva actualización manual, carga, movimientos vacíos, expiración, acceso denegado, fallos de conexión y errores de servidor. «Sin deuda pendiente» requiere un cero numérico explícito y visible en la respuesta que procesa el repositorio. Una respuesta ausente o un error nunca se convierte en un saldo cero.

## 6. Pruebas y APK

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` finalizó correctamente. Pasaron 258 pruebas JVM, incluidas nueve de pagos, sin fallos ni omisiones. El primer intento detectó que la barra de pagos necesitaba declarar el uso de la API experimental de Material 3. Se corrigió y se repitieron las tres tareas. `git diff --check` terminó sin errores.

El APK de prueba se entrega en `app/build/outputs/apk/debug/app-debug.apk`. Las pruebas instrumentadas se empaquetan en `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. `adb devices` no detectó teléfonos conectados y `emulator -list-avds` no encontró emuladores configurados. La navegación en pantalla, la interacción con Canvas, el login con cuenta legítima y el cambio de cuenta en Android siguen pendientes. Las pruebas JVM cubren el comportamiento local, incluidos los seis servicios simulados y el aislamiento de sesión del cliente compartido. Los contratos de transporte de pagos siguen pendientes de pruebas MockWebServer tras obtener evidencia.

## 7. Actualización e instalación local

El identificador es `com.example.upaos` y la versión es 1.7.9, código 25. Android permite reemplazar una instalación con el mismo identificador, un certificado compatible y un código de versión igual o superior al instalado. [Reglas de actualización de Android](https://developer.android.com/google/play/app-updates)

`apksigner verify --verbose --print-certs` confirmó una firma válida y el mismo SHA-256 de certificado del APK debug local de la Fase 2, `93fec1953fbc6832254ada325cdf3bcac70bbaaaf4a43abe4982d1d88361228e`. `aapt dump badging` confirmó el identificador, la versión, minSdk 24 y targetSdk 37. El APK puede reemplazar el debug local anterior bajo las reglas de Android. El certificado de la app instalada en el teléfono quedó sin comparar y el entorno carece de la configuración de firma de distribución.

1. Conectar el teléfono por USB con depuración habilitada y aceptar la autorización que solicita Android. Ejecutar `adb devices` desde el directorio `platform-tools` del SDK o con ADB en el PATH.
2. Desde la raíz del repositorio ejecutar `adb install -r app/build/outputs/apk/debug/app-debug.apk` si la firma coincide. Si Android rechaza la firma, usar un dispositivo de prueba disponible o generar un APK con la clave de la instalación mediante el flujo local de distribución. Desinstalar la app existente borraría sus datos locales. [Guía de firma de UPAO S](DISTRIBUCION_SEGURA.md)
3. Abrir UPAO S e iniciar sesión con la cuenta propia. Recorrer Hoy, Cursos, Horario y Asistencia. Abrir la quinta posición, elegir Canvas, probar un curso y volver. Elegir Pagos, confirmar el aviso de demostración, cambiar el periodo, actualizar y usar Atrás. Revisar que ambas opciones vuelvan a abrir y que el cambio de cuenta no conserve el estado anterior de Pagos.

## 8. Evidencia pendiente y siguiente fase

En una sesión propia, abrir Chrome DevTools → **Network**, activar **Preserve log** y registrar **Doc** y **Fetch/XHR** mientras se abre Estado de cuenta desde una sesión de notas. Para cada una de las seis rutas, compartir método, host y ruta, estado, Content-Type, encabezados funcionales, ubicación y tipos de parámetros, y forma del JSON de éxito y vacío. Conservar las diferencias entre un campo vacío, nulo y omitido.

Registrar los saltos de autenticación e inicialización por estado, host y ruta. Para cookies, compartir solo nombres, dominio, ruta y atributos. Para CSRF, compartir nombre y origen. Omitir valores de cookies, contraseñas, tokens e identificadores. Reemplazar importes y datos personales por valores sintéticos del mismo tipo. En movimientos y selector, capturar la primera página y la siguiente cuando exista, el final de la lista y la opción «Todos los periodos» si aparece. Para expiración, aportar la forma y estado de un ejemplo legítimo si se observa.

Con esas capturas se puede confirmar el acceso y crear `StudentSelfServicePaymentService` con cancelación, validación del destino y JSON, pruebas MockWebServer y verificación con cuentas autorizadas en Android.

## RESUMEN TÉCNICO PARA CHATGPT

La Fase 3 mueve Estado de cuenta a la quinta posición del `HorizontalPager` existente, mediante un menú Canvas/Pagos. Reutiliza la pantalla, el ViewModel y el repositorio, y conserva el origen de navegación para Atrás. El ViewModel sigue acotado a la generación de sesión y el proveedor sigue leyendo el recurso de demostración. Los métodos y la autenticación de las seis rutas reales permanecen sin confirmar. El informe documenta el APK local, la verificación de firma, las pruebas y la captura necesaria para implementar el adaptador directo.
