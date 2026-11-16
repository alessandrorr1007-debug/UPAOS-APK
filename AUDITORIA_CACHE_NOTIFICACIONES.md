# Auditoría de caché y notificaciones — UPAO S

Fecha: 6 de noviembre de 2026 (America/Lima). Trabajo local en `main`, sin commits ni pushes.

## Alcance y evidencia

Se inspeccionaron almacenamiento, repositorios Banner/Canvas, dashboard, pantallas académicas, autenticación, limpieza de sesión, WorkManager, servicio intensivo de notas, recordatorios, permisos y manifiesto. Los cambios de documentos Canvas que ya existían al empezar se conservaron.

Los hallazgos siguientes se demuestran por el flujo del código. No se dispone de registros del teléfono que presentó el problema, ni de un dispositivo/emulador conectado. Por ello no se atribuye un retraso concreto del teléfono a Doze, fabricante o permisos sin medirlo.

## A. Diagnóstico inicial

| Problema | Archivo/componente | Causa encontrada | Evidencia del flujo anterior | Severidad |
|---|---|---|---|---|
| Caché esperaba trabajo de red | `DashboardViewModel.refreshRound` | `loadCache()` estaba dentro de `ForegroundRefreshGate.lock` | Un worker que poseía el mutex impedía iniciar la lectura local | Alta |
| Arranque artificialmente demorado | `MainActivity.SplashScreen` | Espera fija de 1.200 ms incluso con sesión guardada | `delay(1200)` antes de navegar | Media |
| Resultados nuevos aparecían tarde | `DashboardViewModel` | Publicación de la ronda completa al final | Notas esperaban asistencia, horario, agenda y otros endpoints | Alta |
| Consultas repetidas de desgloses | Dashboard / `UpaoRepository` | El repositorio enriquecía cursos y el dashboard descargaba de nuevo cada detalle | `buscarNotas` y luego `getDetalleCurso` para los mismos CRN | Media |
| Fecha de notas ausente al restaurar | `DashboardViewModel.loadCache` | No se leía el timestamp de `GradesCache` | `times` solo incluía claves de `ApiCache` | Media |
| Horario de otro periodo etiquetado como actual | `DashboardViewModel.loadCache` | Fallback `horario_ultimo` sin verificar su periodo | `putIfAbsent(current, ultimo)` incondicional | Alta |
| Cursos Canvas podían competir con restauración | `CanvasHubViewModel` | Acciones públicas podían iniciar red antes de terminar de leer disco | Los fetch no esperaban la inicialización | Media |
| Consulta duplicada de cursos Canvas al iniciar | `CanvasHubViewModel` | `fetchCourses` y `fetchTodo` consultaban cursos | La construcción de pendientes ya obtiene los cursos | Media |
| Asistencia podía dejar de revisarse indefinidamente | `SyncScheduler` / `AsistenciaWorker` | Cadena de OneTimeWork dependiente de que cada ejecución encolara su sucesora | Error fatal producía duración cero; cancelación/interrupción antes de reprogramar rompía la cadena | Alta |
| Revisiones omitidas con Activity visible | Ambos workers | Salida por `ForegroundRefreshGate.active` aunque el dashboard no ejecuta la comparación de cambios | La interfaz actualizaba datos sin sustituir la detección del worker | Alta |
| Respaldo de notas dependía del servicio intensivo | `GradeBackupWorker` | Intentaba iniciar un servicio desde segundo plano y podía salir por `running` | `reconcile(allowBackgroundStart=true)` antes de consultar | Alta |
| Periodo de notificaciones podía quedar antiguo | `GradeSyncCheck` | Solo refrescaba periodos si el valor persistido era nulo | Sin abrir dashboard podía seguir consultando un periodo viejo | Alta |
| Dedupe suprimía un cambio legítimo posterior | `GradeSyncCheck.emitGrade` | Huella permanente por valor de nota, sin generación de snapshot | Una secuencia de cambios que volvía al mismo valor encontraba una huella antigua | Media |
| Permisos/canales bloqueados no tenían diagnóstico central | `NotificationService` | Se enviaba sin comprobar permiso y canal en ese punto | Un fallo podía confundirse con error de sincronización | Alta |

## B. Correcciones implementadas

1. Restauración del dashboard en un job independiente del mutex de red. Las solicitudes esperan su finalización; un worker ocupado ya no impide leer disco.
2. Se omite la pausa del splash cuando existe sesión recordada y usuario identificado. Se mantiene el bloqueo de seguridad de la app.
3. Cada fuente completada publica su resultado sin esperar toda la ronda. Una selección de periodo/carrera hecha durante la consulta mantiene prioridad.
4. Se reutilizan los componentes ya descargados para calcular el progreso; no se altera la fórmula de calificaciones.
5. Se restaura la fecha de notas y se exige coincidencia del periodo para el horario de respaldo.
6. Los fetch del hub Canvas esperan la restauración; el arranque reutiliza la consulta de cursos del flujo de pendientes.
7. Asistencia usa trabajo periódico único persistente de 15 minutos, flex de 5 minutos, conectividad y backoff exponencial. Una fecha por cuenta conserva el descanso adaptativo. Un error fatal pospone una hora y no destruye la programación.
8. Las acciones manuales usan trabajos únicos con KEEP y red requerida; no cancelan una ronda en curso. Se migran/cancelan exclusivamente nombres de trabajos antiguos, sin borrar caché.
9. Los workers no se omiten solo por estar visible la app. Si el mutex está ocupado, solicitan retry. El worker de notas consulta directamente sin arrancar el servicio.
10. El controlador solo inicia el servicio intensivo existente desde visibilidad de la aplicación. Se conserva esta función previa, con sus límites Android; no se añade otro servicio.
11. Metadatos de periodo de notas se revalidan como máximo diariamente, compartiendo la fecha obtenida por las otras consultas de periodos.
12. Las notas comparan snapshots compatibles con el periodo. Snapshots y generaciones se guardan juntos mediante commit. La generación distingue eventos repetidos legítimos; la huella conserva dedupe durante reintentos de una misma ronda.
13. Asistencia también registra huellas persistentes. Los avisos de ambos tipos usan identificadores deterministas y `setOnlyAlertOnce` para reducir duplicados si el proceso muere entre emitir y persistir.
14. Se comprueban permiso Android 13+, bloqueo global y canal. El cambio de permisos durante `notify` se controla en los avisos de notas/asistencia. Un aviso bloqueado no se cuenta como emitido.
15. Se valida la cuenta antes de recuperar contraseña para reautenticar una solicitud Banner; se valida la sesión del worker de notas después del login.
16. Ajustes muestra intentos, éxitos, resultados técnicos, errores y contadores de entrega. La indicación de batería abre ajustes generales y explica el posible retraso sin solicitar una exención directamente.

## C. Persistencia y estrategia de caché

### Almacenamiento existente conservado

| Datos | Almacenamiento | Alcance |
|---|---|---|
| Notas y proyecciones | DataStore `notas_cache` | Claves por usuario; proyecciones por CRN |
| Periodos, horario, asistencia, semana, agenda e historial local | DataStore `api_cache` | Claves por usuario; horarios por periodo |
| Cursos, pendientes, conversaciones, grupos, avisos y recursos Canvas | DataStore `canvas_cache` | Claves por usuario y recurso |
| Referencias de comparación, dedupe y diagnóstico | SharedPreferences `upaos_notification_snapshots_<usuario>` | Una referencia por cuenta |
| Credenciales y tokens | `SecureStore`, Android Keystore / AES-GCM | Se conserva la infraestructura existente |
| Variantes de notas, promedios y selección de pestañas | Memoria del ViewModel; parte de históricos en ApiCache | No todas las variantes de carrera/recurso tienen persistencia universal |

Antes había persistencia real, pero la lectura local quedaba detrás de la exclusión de red. Ahora la secuencia es: cuenta identificada → restauración local independiente → estado visible → mutex de red → revalidación progresiva. La primera apertura sin datos sigue necesitando red. Las pantallas académicas existentes solo sustituyen contenido por carga completa cuando no tienen datos que mostrar.

Se preservan los formatos y nombres de DataStore, incluida la clave histórica doble `notas_notas_<usuario>` que resulta de la API de GradesCache. No se hace una migración destructiva ni se introduce Room. Los campos nuevos de preferencias tienen valores predeterminados compatibles.

Errores y respuestas incompletas siguen pasando por `RefreshResult` / `requireComplete`: no se publican como listas vacías. Una respuesta completa legítimamente vacía sí puede reemplazar una lista. Canvas conserva las fuentes fallidas en actualizaciones parciales.

Cerrar el proceso o reiniciar no borra DataStore. La revalidación fallida conserva la información anterior y sus fechas. El indicador de actualización distingue datos guardados de datos revalidados; no se presenta como oficial una nota de otro periodo.

La política previa se mantiene: logout invalida sesión, cookies y trabajos; conserva datos de una cuenta guardada. Eliminar cuenta o usar la opción de borrar datos locales elimina su caché y referencias. No se cambió esta política ni se borró información de usuarios como reparación. El sandbox privado y `allowBackup=false` protegen los archivos; el JSON académico de DataStore no tiene cifrado adicional propio. Las credenciales sí usan Keystore.

Quedan límites previos: no todas las variantes históricas por carrera se restauran inmediatamente desde disco, y no todos los recursos Canvas tienen caché persistente. Esto no equivale a una garantía de caché universal para cada pantalla.

## D. Notificaciones y puntualidad

### Arquitectura y frecuencias

- Notas: respaldo WorkManager único cada 15 minutos, red requerida y reintento en fallos transitorios/contención. El servicio intensivo previo conserva su cadencia aproximada de dos minutos cuando está autorizado por la configuración y puede iniciarse desde la app visible.
- Asistencia: periódico único cada 15 minutos con flex de 5; el filtro adaptativo conserva pausas de aproximadamente 60 minutos por noche, domingo o fuera del ciclo, y backoff ante errores. El tiempo efectivo puede superar el umbral hasta el siguiente turno que conceda Android.
- Agenda: worker previo cada 15 minutos cuando el usuario configuró recordatorios de entregas. No se añaden avisos de nuevas tareas o anuncios que antes no existían como categoría local.
- Clases: se conserva el programador de recordatorios existente; no se sustituye por polling ni se añaden permisos.
- Reinicio y actualización: WorkManager conserva su base de trabajos; `BackgroundSyncReceiver` reconcilia la programación existente. No requiere Activity para consultar Banner o emitir un aviso autorizado.
- Cuenta activa: los trabajos comprueban usuario/generación de sesión. No se habilita polling simultáneo de todas las cuentas guardadas.

### Por qué podía mejorar al abrir la app

Abrirla volvía a ejecutar `SyncScheduler.start`, podía reconstituir la cadena perdida y hacía visible el contexto desde el que se iniciaba el servicio intensivo. Además, el dashboard actualizaba el periodo que el comparador de notas podía haber conservado antiguo. Estos son caminos concretos del proyecto, independientes de atribuir todo a batería.

Esto explica mecanismos compatibles con el síntoma; para determinar cuál ocurrió en un teléfono particular se necesitan sus registros. No se ha medido aquí la latencia real ni se ha reproducido un timeout OEM.

### Restricciones que permanecen

El mínimo de trabajo periódico es 15 minutos y su ejecución no es exacta. Doze, restricciones de batería, buckets de inactividad y políticas del fabricante pueden retrasarlo. Forzar detención desde Ajustes no equivale a retirar de recientes; puede impedir ejecución hasta volver a abrir la aplicación. Los servicios dataSync tienen restricciones de inicio y límite acumulado en Android moderno. No se intenta eludirlos.

Los cambios bloqueados por permisos quedan registrados en diagnóstico/historial; no se promete reenviar retrospectivamente cada aviso al conceder permisos. El sistema de notificaciones y la persistencia no forman una transacción distribuida: los IDs estables y las huellas reducen duplicados, pero no ofrecen exactamente una entrega bajo toda caída posible. El historial local previo todavía puede duplicarse si una interrupción sucede durante su registro.

### Diagnóstico

Logcat: etiquetas `UPAO_SyncDiagnostic`, `UPAO_GradeSync`, `UPAO_BackgroundSync`. Incluyen identificador de trabajo, intento, inicio, duración, resultado, conectividad, categoría de error y entrega emitida/bloqueada. Se guarda por cuenta el último intento/éxito y contadores; no se guardan tokens, contraseña, contenido académico ni mensajes crudos de excepciones en este diagnóstico.

La ausencia de un intento nuevo no demuestra por sí sola Doze ni que el proceso haya sido forzado a detenerse. Para separar programación, restricciones y ejecución hace falta combinar el diagnóstico con WorkManager/JobScheduler del dispositivo. Tampoco existe una marca de publicación de origen fiable para medir el tiempo transcurrido antes de que Banner exponga un cambio.

### Alternativas evaluadas

| Alternativa | Viabilidad y latencia | Costos, seguridad y mantenimiento | Recomendación |
|---|---|---|---|
| WorkManager local | Compatible con las consultas Banner/Canvas existentes; latencia de polling más retraso Android | Sin servidor adicional; credenciales quedan en el dispositivo; consumo ligado a consultas | Base implementada y conservada |
| FCM con backend | FCM transporta avisos, pero no detecta por sí mismo cambios en Banner/Canvas | Requiere entorno servidor de confianza, gestión de dispositivos, autorización, operación y posibles costos de infraestructura | No implementado: no hay fuente institucional autorizada demostrada |
| Híbrida | Push para señales autorizadas y WorkManager para comprobar/recuperar estado | Mayor complejidad; minimizar datos en payload; no trasladar contraseñas del estudiante a un backend improvisado | Evaluar con autorización institucional y mediciones de latencia |

Canvas ofrece Live Events con configuración/permisos de cuenta y destinos de recepción. No se ha demostrado acceso a ellos para esta app; además, su documentación no los recomienda para datos que deban ser inmediatamente actuales. No se encontró una integración de eventos Banner autorizada en el proyecto. Por tanto no se puede prometer que añadir FCM resolvería la detección ni su puntualidad.

Fuentes oficiales consultadas:

- [WorkManager: trabajo periódico y Doze](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)
- [Definición de requests, constraints y flex](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Restricciones dataSync en Android 15+](https://developer.android.com/about/versions/15/behavior-changes-15)
- [FCM: entorno servidor de confianza](https://firebase.google.com/docs/cloud-messaging/server-environment)
- [Canvas: configuración de Live Events](https://developerdocs.instructure.com/services/canvas/data-services/live-events/overview/file.data_service_setup)
- [Canvas: alcance y limitaciones de Live Events](https://dhs.instructure.com/doc/api/file.data_service_introduction.html)

## E. Pruebas y limitaciones

Suite JVM ejecutada: **237 pruebas, 237 aprobadas, 0 fallos, 0 errores, 0 omitidas**. Incluye pruebas unitarias y pruebas de transporte con MockWebServer; MockWebServer no representa una conexión real a UPAO.

Lint adicional: **1 error, 143 advertencias y 2 sugerencias**. El error es `UnspecifiedRegisterReceiverFlag`, en `AppUpdater.kt:164`, rama de compatibilidad del receptor `DOWNLOAD_COMPLETE`. La misma instrucción ya está en HEAD; ese archivo no se modificó. El código sí utiliza `RECEIVER_EXPORTED` en Android 13+, por lo que el diagnóstico estático no demuestra por sí mismo un fallo en Android 14. No se añadió baseline ni se suprimió el análisis. `lintDebug` terminó con código de error; se informa separado de las tareas de compilación/pruebas satisfactorias.

Pruebas nuevas JVM en `CanvasViewModelTest`:

1. Caché visible mientras la actualización está suspendida y publicación posterior de datos nuevos.
2. Dos recreaciones de ViewModel sin conexión sin sobrescribir la referencia válida.
3. Respuesta completa vacía aceptada como eliminación legítima.
4. Solicitud manual durante restauración espera al disco y se combina con el arranque.

Tres pruebas instrumentadas nuevas en `AcademicCachePersistenceTest`: recreación/persistencia de notas, horario y cursos; limpieza aislada por cuenta; fecha adaptativa por cuenta. La primera permite fases `cachePhase=write/read` en procesos separados. **Solo compiladas; no ejecutadas.**

Cobertura y pendientes de caché:

| Casos solicitados | Evidencia disponible | Validación restante |
|---|---|---|
| Primera apertura sin caché | Suite de recursos y respuestas vacías | Arranque completo en dispositivo |
| Segunda apertura, sin conexión y conexión lenta | Nuevas pruebas de hub con estado observable | Medir arranque real Banner y Canvas |
| Error Banner/Canvas, respuesta incompleta o vacía | RefreshIntegrityTest, CanvasRefreshIntegrityTest, HTTP/paginación y nuevas pruebas del hub | Fallos reales controlados en dispositivo |
| Cierre/reapertura y muerte de proceso | Recreación JVM; prueba instrumentada preparada por fases | Ejecutar fases en procesos distintos |
| Cambio de cuenta y logout | AccountSessionTest, SessionCleanupTest, ReminderOwnershipTest; instrumentadas preparadas | Cambio de cuenta visual durante solicitudes en vuelo |
| Actualización de datos antiguos y ausencia de estado vacío | Estado conservado durante refresh en pruebas JVM | Verificación visual y tiempos reales del dashboard |
| Integridad tras actualizar APK | Formatos anteriores conservados; migración aditiva | Instalar actualización firmada sobre versión anterior sin desinstalar |

Para las 20 condiciones de notificaciones solicitadas —abierta, segundo plano, recientes, bloqueada, reposo, ahorro, Wi-Fi, datos, offline, reconexión, reinicio, sesión vencida, token revocado, cambios, sin cambios, permisos denegados/concedidos, varias cuentas, retraso Android e inicio tras inactividad— **no se declara una aprobación en dispositivo**. Las pruebas JVM cubren comparación/políticas, autenticación simulada, aislamiento y fallos de transporte; no ejecutan el planificador real Android.

No se incorporó una simulación completa de WorkManager mediante work-testing. La continuidad del periódico está sustentada por la API y la inspección, pero falta verificarla en Android. La aceptación de puntualidad queda pendiente.

Protocolo de dispositivo:

1. Instalar una actualización compatible con la firma existente, sin borrar datos. Registrar cuenta activa, permiso y canales. Confirmar datos guardados antes de desconectar.
2. Comparar arranque online/offline; registrar tiempo hasta contenido y fecha mostrada. Introducir red lenta y comprobar que los datos no desaparecen.
3. Consultar WorkManager/JobScheduler y Logcat filtrado por las etiquetas indicadas. Comparar hora del intento, éxito, error y emisión.
4. Repetir con recientes, pantalla bloqueada, reposo, ahorro, Wi-Fi/móvil y recuperación de red. Separar expresamente forzar detención.
5. Reiniciar y actualizar APK sin abrir inmediatamente la interfaz; comprobar trabajo persistido y cuenta correcta.
6. Simular expiración/revocación de forma autorizada; comprobar error técnico sin reemplazar caché ni referencia por datos vacíos.
7. Comparar cambios controlados con ausencia de cambios, permisos bloqueados y cambio de cuenta en vuelo. Medir desde disponibilidad del dato en origen hasta aviso.

## F. Compilación

Se usó JDK de Android Studio mediante `JAVA_HOME` solo para el proceso de compilación.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
```

APK debug: `D:\Proyectos\UPAO PROC\UPAO S\app\build\outputs\apk\debug\app-debug.apk`.

Es un APK de depuración. No se generó ni se firmó una distribución de producción con credenciales privadas. No debe asumirse que su firma permite actualizar la instalación de producción.

Registros de esta auditoría: `app/build/reports/audit/`. Informe JVM: `app/build/reports/tests/testDebugUnitTest/index.html`. Resultado de lint: `app/build/reports/lint-results-debug.html` y XML correspondiente. La primera tentativa detectó un argumento faltante en una prueba nueva; se corrigió antes de la ejecución de 237 pruebas aprobadas.

## G. Estado final

Corregidos en código: bloqueo de lectura local por red, pausa de sesión recordada, publicación tardía por ronda, consultas duplicadas identificadas, fecha de notas, fallback de horario sin periodo, competencia de hidratación Canvas, fragilidad de cadena de asistencia, omisión por visibilidad, arranque del servicio desde worker, metadatos de periodo antiguos, dedupe por valor permanente y diagnóstico de bloqueos.

Parcial: observabilidad de retrasos (sin historial completo de WorkInfo/JobScheduler), dedupe del historial local y cobertura de caché de todas las variantes históricas. Conservado: servicio intensivo opcional previo con límites Android, política de privacidad y funciones existentes.

Pendiente: pruebas instrumentadas/reales de arranque, cambio de cuenta visual, persistencia entre procesos/actualización, reinicio, OEM, Doze, batería y latencia efectiva. No hay evidencia suficiente para marcar todos los criterios de aceptación como cumplidos en un teléfono.
