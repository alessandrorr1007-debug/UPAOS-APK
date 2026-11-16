# Fase 2 de pagos reales de UPAO S

## 1. Resumen ejecutivo

Se auditó el módulo Estado de cuenta, la sesión académica de Banner, el código local y las fuentes públicas. Las seis consultas siguen disponibles en la demostración. Para habilitar la lectura institucional faltan las solicitudes observadas y respuestas redactadas de una sesión autorizada. Las ampliaciones de pruebas ejercitan el límite local de `PaymentService`, el repositorio y el ViewModel.

La documentación pública de UPAO describe el acceso a Estado de cuenta desde UPAO IN y el detalle de saldos. Ellucian documenta la función Student Account dentro de Banner Student Self-Service. Ninguna de las dos fuentes consultadas especifica los seis contratos del despliegue UPAO. [Manual de Pagos Virtuales UPAO](https://static.upao.edu.pe/upload/manuales/Manual_de_Usuario_Pagos_Virtuales_v2_1.pdf#page=24) · [Descripción de Banner Self-Service de Ellucian](https://www.ellucian.com/assets/emea-ap/brochure/emea-brochure-learn-more-about-ellucian-banner-self-service.pdf#page=2)

## 2. Auditoría del código

- `INFORME_INTEGRACION_PAGOS_UPAO.md` registra el alcance y las verificaciones previas. La búsqueda de los nombres exactos en el código, la documentación, el historial local y fuentes públicas no produjo un contrato independiente para pagos. El adjunto de esta fase contiene la consigna. El proyecto carece de capturas HAR de estas operaciones.
- `PaymentService` define las seis operaciones suspendidas. `DemoPaymentService` lee el recurso local `app/src/main/assets/payments_demo.json` y Gson lo convierte a los DTO de `PaymentDtos.kt`. Sus campos anulables y `JsonElement` reflejan supuestos de la demo y formas aún indeterminadas.
- `PaymentsRepository` selecciona moneda, relaciona totales con periodos, toma el código de la selección y mapea los movimientos. Su orden actual es moneda → saldo → totales → selector → retenciones → movimientos al tener periodo. Esa secuencia pertenece al repositorio local. La del portal sigue por observar.
- `PaymentsViewModel` cancela consultas reemplazadas, limpia el estado en 401/403 simulados y expone carga, datos y errores. `PaymentsScreen` muestra la demo y llama a `clear()` al salir de composición. `MainActivity` crea el ViewModel en la entrada de navegación y usa la generación de sesión como clave.
- El punto de conexión de un adaptador real sigue siendo `PaymentService`. La comunicación objetivo es directa entre Android y `https://ssb.upao.edu.pe`.

## 3. Tabla de los seis endpoints

Prefijo común `https://ssb.upao.edu.pe/StudentSelfService/ssb/`. Las rutas y los nombres de parámetros provienen del encargo. Para las seis filas siguen indeterminados el método, la ubicación de parámetros, su obligatoriedad salvo la observación previa sobre `termCode`, las cabeceras funcionales, el Content-Type, el esquema y los tipos reales, los códigos de error y la autenticación. El método de la página de notas no permite deducir ninguno de esos datos.

| Servicio y ruta bajo el prefijo | Método | Parámetros reportados | Autenticación | Respuesta preparada y estado |
| --- | --- | --- | --- | --- |
| Resumen · `accountSummary/getBalanceAndConfigInfo` | Desconocido | `currencyCode` vacío en la observación previa, `isOverview=true` | Por confirmar | `BalanceDto` con saldo, pendiente y fecha. Falta observar `currency`, `data` y los indicadores de visibilidad. Preparado con datos simulados. |
| Periodos y totales · `accountSummaryByTerm/getMyTermsAndTotals` | Desconocido | `currencyCode` | Por confirmar | `TermBalancesDto` con totales por `termCode`. Falta confirmar el cruce con el selector y las banderas `cannotExpand` y `clickable`. Preparado con datos simulados. |
| Movimientos · `accountSummaryByTerm/fetchAccountSummaryByTerm` | Desconocido | `currencyCode`, `itemCode`, `max`, `offset`, `onPage`, `pageSize`, `searchString`, `takeFromCache`, `termCode` (reportado como requerido) | Por confirmar | `MovementsDto` con filas y totales. Falta confirmar `length`, el alcance de `totals`, `itemCode` y la paginación. Preparado con datos simulados. |
| Retenciones · `studentHold/getHoldCount` | Desconocido | Ninguno identificado en la URL reportada. Cuerpo y cabeceras por confirmar. | Por confirmar | `HoldsDto` con `count` y `noHolds`. Falta observar tipos y variantes. Preparado con datos simulados. |
| Selector · `accountSummaryByTerm/getMyTerms` | Desconocido | `currencyCode`, `loadNext`, `max`, `offset`, `searchString` | Por confirmar | `TermsDto` con `dropDown`. Faltan la clave agregada, los límites, la búsqueda y el comportamiento al pedir otra página. Preparado con datos simulados. |
| Monedas · `currency/listForUIForStudent` | Desconocido | Ninguno identificado en la URL reportada. Cuerpo y cabeceras por confirmar. | Por confirmar | `CurrenciesDto` con `result`. Falta confirmar cuándo usar `key`, `iso` o vacío, y la secuencia de inicialización. Preparado con datos simulados. |

## 4. Análisis de autenticación

`LoginScreen` y `EligeCuentaScreen` usan el flujo existente de `UpaoAuth`. En el código, ese flujo parte de un GET a `/StudentSelfService/ssb/studentGrades`, procesa el formulario SSO y considera éxito el aterrizaje en el host SSB bajo `/StudentSelfService/ssb/`. El ping de sesión solicita JSON a `studentGrades/term`, exige identidad coincidente en memoria y valida host y tipo de respuesta. La lista de confianza del login incluye `ssb.upao.edu.pe` y `upaosso.upao.edu.pe` por HTTPS. La inscripción y Canvas siguen flujos propios.

`TokenManager.saveLogin` guarda el usuario y una generación de sesión nueva. El campo local `auth_token` se inicializa con el identificador del usuario en este flujo. `SecureStore` protege los valores persistidos mediante Android Keystore. La sesión web se mantiene en memoria en `UpaoHttpClient.AccountSession`. El jar gestiona el alcance por dominio, ruta y atributos de las cookies recibidas. Los valores concretos de las cookies y su alcance en producción quedan fuera de la evidencia disponible. Al seleccionar otra cuenta o cerrar la sesión, se invalidan el jar y las llamadas de la cuenta anterior. El interceptor comprueba también la validez al consumir el cuerpo.

Lo anterior describe el mecanismo de acceso de UPAO S a notas de Student Self-Service. `BannerApi` trata 401/403, destinos de login y ciertas señales de formulario en HTML como indicios de expiración en sus consultas. La compatibilidad con pagos, la inicialización adicional y la forma real de detectar expiración quedan indeterminadas. La clasificación 401/403 de `PaymentsRepository` es una regla local. Para pagos deben observarse los códigos, el destino y la cadena de redirección, o el tipo y la forma de un eventual HTML de autenticación con estado 200.

El 12 de noviembre de 2026, un GET anónimo a la página de notas con el User-Agent y el Accept de `UpaoAuth` observó esta cadena pública. Se usó `System.Net.Http.HttpClient` con cookies deshabilitadas y redirecciones seguidas manualmente solo entre los tres hosts UPAO observados por HTTPS. Se registraron únicamente estados, host, ruta y nombres de campos.

1. HTTP 302 en `ssb.upao.edu.pe/StudentSelfService/ssb/studentGrades`.
2. HTTP 302 en `login.upao.edu.pe/cas/login`.
3. HTTP 302 en `login.upao.edu.pe/commonauth`.
4. HTTP 200 `text/html` en `upaosso.upao.edu.pe:410/Account/Login`, con los nombres de campos `id_usuario`, `nip` y `__RequestVerificationToken`.

El salto intermedio por `login.upao.edu.pe` es compatible con que el cliente de notas siga redirecciones y compruebe el destino del formulario al que se enviarían las credenciales. El host del formulario final sí pertenece a la lista de confianza de `UpaoAuth`. `ExperienceAuth` reconoce el intermediario en su propio flujo. Esta observación confirma la entrada pública de notas hasta el formulario. El retorno autenticado, las cookies emitidas y las seis operaciones financieras siguen pendientes de observación.

## 5. Archivos modificados

- `app/src/test/java/com/example/upaos/PaymentsTest.kt` amplía la suite de pagos con casos locales.
- `INFORME_FASE2_PAGOS_REALES_UPAO.md` documenta evidencia, verificación y capturas faltantes.

## 6. Pruebas

- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --rerun-tasks --console=plain` recompiló la línea base con el JDK de Android Studio. Pasaron 253 pruebas JVM, incluidas las cuatro originales de pagos. Se generaron los APK de depuración y de pruebas instrumentadas.
- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` pasó con 257 pruebas JVM, cero fallos y cero omisiones. Las ocho pruebas de pagos incluyen las cuatro nuevas de listas vacías, rechazo de documentos vacíos/incompletos/HTML, clasificación y recuperación tras 403/500/503/timeout simulado, y descarte de una respuesta tardía de un ViewModel de sesión anterior. También pasaron las tres pruebas existentes de `AccountSessionTest`.
- `git diff --check` terminó sin errores. `adb devices` no mostró dispositivos conectados y `emulator -list-avds` no listó emuladores. Los APK se compilaron, con ejecución instrumentada y verificación autorizada en Android pendientes.

La suite preexistente incluye pruebas MockWebServer de `AccountSession` para cambio de cuenta con una respuesta en curso, antes y después de recibir encabezados. Esas pruebas cubren la infraestructura compartida. La suite de pagos usa JSON sintético y errores inyectados a través de `PaymentService`. La detección HTTP de redirecciones SSO, Content-Type, timeout y paginación específicos de pagos queda pendiente del adaptador y de evidencia del portal. Una excepción simulada de timeout verifica su presentación como error de conexión, y un cuerpo HTML sintético verifica el rechazo del parser local.

## 7. Bloqueos

Falta observar las seis solicitudes durante una sesión autorizada y disponer de un dispositivo para la validación final. Los siguientes datos determinan el adaptador: método y codificación por operación, secuencia de inicio y permisos, procedencia de cabeceras, cookies o CSRF, esquemas de éxito y fallo, orden de dependencias y señales de paginación. Las trazas de notas y los documentos públicos no resuelven esos puntos.

## 8. Estado final

Los seis servicios se clasifican como **Preparado con datos simulados**, según la tabla de la sección 3. La activación de la integración real está bloqueada por información insuficiente para los seis. El destino muestra el aviso de demostración.

## 9. Instrucciones para continuar

1. En una sesión propia y legítima, abrir Chrome DevTools → **Network** y activar **Preserve log**. Registrar **Doc** y **Fetch/XHR**. Entrar por el flujo habitual de notas y luego abrir Estado de cuenta. Anotar en qué orden se ejecutan las solicitudes y si se observa una inicialización propia de pagos. Si el portal también ofrece una entrada directa a Estado de cuenta, registrar esa navegación en una sesión normal.
2. Capturar para cada una de las seis rutas lo que aparece en **Headers**, **Payload** y **Response**: método, host y ruta, estado, Content-Type de solicitud y respuesta, nombres y tipos de parámetros, y ubicación en query, formulario o JSON. Conservar la distinción entre vacío, nulo y omitido. Incluir solo los nombres de cabeceras funcionales y el origen de CSRF si existe. Para cookies, conservar nombres, dominio, ruta y atributos con sus valores eliminados. Para redirecciones, conservar estado y destino sin parámetros sensibles.
3. Abrir un periodo, «Todos los periodos» si aparece, un periodo vacío si está disponible y la página siguiente cuando exista. Para movimientos, anotar los cambios de `offset`, `max`, `onPage`, `pageSize`, `takeFromCache`, `length` y totales. Para el selector, registrar también el efecto de búsqueda y carga siguiente si el portal los ofrece. Para monedas, anotar si el portal consulta con clave, ISO o vacío.
4. Compartir ejemplos estructurales de éxito y vacío con los mismos nombres, anidamiento, tipos, signos y nulos. Si se observa expiración o acceso denegado en el uso habitual, incluir estado, Content-Type, redirección y forma del cuerpo. Para los datos sensibles, usar valores sintéticos que conserven el tipo. Excluir credenciales, tokens, valores de cookies, identificadores estudiantiles, importes personales, HAR sin revisar y comandos copiados como cURL sin redactar.
5. Con la evidencia, confirmar la sesión y crear un único adaptador suspendido de `PaymentService`. Probar las seis solicitudes y su transporte con MockWebServer. Mantener la selección de la demo hasta verificar el adaptador en Android con sesiones autorizadas y comprobar el cambio entre cuentas.

## 10. Resumen para ChatGPT

La Fase 2 auditó los seis nombres de servicio de Student Self-Service, la pantalla demo y la sesión de notas. La fuente activa sigue siendo el recurso sintético, a través de `DemoPaymentService`. `UpaoAuth` y `AccountSession` son candidatos a reutilización solo tras confirmar el flujo de pagos. El futuro prompt debe partir de las capturas redactadas de la sección 9 y utilizar los DTO, repositorio, ViewModel y pantalla existentes. Debe cubrir las seis rutas, aplicar cancelación e identidad de cuenta al transporte, validar resultados con MockWebServer y Android, y condicionar la activación a una verificación autorizada.
