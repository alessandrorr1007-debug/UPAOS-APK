# UPAO S — estabilización y verificación final

6 de noviembre de 2026, America/Lima. Trabajo local sobre `main`, sin commits, pushes ni ramas nuevas.

## Estado inicial y método

Se leyó `AUDITORIA_CACHE_NOTIFICACIONES.md` y se contrastó con el código. Las mejoras descritas allí seguían presentes: hidratación independiente de la red, publicación progresiva, coordinación del hub Canvas, periódico de asistencia, respaldo de notas y generación de snapshots. No se reconstruyeron estos módulos.

El repositorio tenía modificaciones locales y archivos nuevos de la fase anterior, incluidos documentos y visor Canvas. Se guardaron estado inicial y hashes de los archivos fuente en `app/build/reports/stabilization/`. No falta ningún archivo que existiera al iniciar. Las modificaciones de esta fase se identifican contra esos hashes, no contra HEAD, que también incluye el trabajo anterior sin commit.

Entorno verificado: Gradle 9.3.1; AGP 9.1.1; Kotlin integrado 2.2.21; lanzador JDK JetBrains 25.0.2; daemon configurado para Java 21. Android SDK con plataformas 35 y 37.0; proyecto minSdk 24, compile/targetSdk 37. El binario de emulador y WHPX están disponibles, pero no hay AVD, imagen de sistema instalada ni dispositivo ADB conectado. No se provisionó ni se utilizó un emulador nuevo.

## Hallazgos y estado

| Problema | Estado anterior | Corrección | Evidencia | Estado final |
|---|---|---|---|---|
| Error Lint de receptor | Registro sin flags en rama anterior a API 33 | `ContextCompat.registerReceiver` con exportación explícita y filtro de acción | `AppUpdater.kt`; se conservan consulta de descarga y validación de APK | Implementado; recepción real pendiente |
| Duplicados del historial al reintentar | ID basado en hora, inserción incondicional | Clave estable por cuenta/fuente/periodo/generación/evento e inserción idempotente | Prueba de recreación JSON y reintento; prueba 12→14→12→14 | Corregido y probado en JVM; disco concurrente pendiente Android |
| Pérdida de avisos o lecturas por concurrencia | Lectura y escritura separadas | Read/modify/write dentro de un solo `DataStore.edit` | `ApiCache.actualizar`, inserciones y marcas de lectura | Corregido; prueba concurrente instrumentada compilada |
| Dashboard podía sobrescribir historial nuevo | Volvía a guardar la copia local consultada | Se elimina esa escritura redundante | Flujo de `notificaciones` en DashboardViewModel | Corregido; concurrencia real pendiente |
| Caché histórica confundía carrera | Clave por periodo, restauración sin comprobar carrera | Clave cuenta/periodo/carrera y validación explícita de esquema | Pruebas de UG/PG, periodos y JSON incompleto | Corregido y probado en parser; flujo visual pendiente |
| Fecha/frescura de otra selección | Una ronda vieja podía entregar su fecha al periodo recién seleccionado | Fecha por variante y retirada de frescura/error de la selección anterior | Publicación final y `selectGrades` | Corregido; pendiente de prueba visual |
| Refresco de recursos ocultaba datos | `loading=true` en actualizaciones automáticas | Distinguir contenido disponible de carga inicial | Pruebas de red suspendida, fallo y caché vacía válida | Corregido y probado en ViewModel |
| Listados útiles solo en memoria | Navegación, archivos y temas del curso | Reutilización de `canvasCachedList` | Factory y persistencia Canvas existentes | Corregido; persistencia instrumentada pendiente |
| Retención de nuevos listados | No había política para estos recursos | Máximo 32 listados por cuenta, 256 KiB por JSON y frescura de 7 días | Pruebas de límites, fechas futuras y vencidas | Política probada; disco pendiente |
| Diagnóstico no veía planificador | Solo preferencias propias | Consulta WorkInfo de periódicos y manuales; estado, intentos, red, elegibilidad, parada | APIs de WorkManager 2.9.1 compiladas | Implementado; ejecución real pendiente |
| Éxito del worker podía ser solo una omisión | No explicaba salidas por sesión, candado o plazo adaptativo | Motivo de omisión separado del éxito de sincronización | Campos y registros técnicos de SyncDiagnostics | Implementado; pendiente de ejecución Android |
| Puntualidad sin abrir Activity | Correcciones anteriores implementadas pero sin prueba móvil | Se conserva arquitectura; no se agrega FCM ni otro servicio | Revisión de workers, scheduler y receptor de arranque | Pendiente de validar en teléfono/emulador |

## Caché y compatibilidad

Se mantienen DataStore y los archivos existentes. Los constructores de ApiCache, GradesCache y CanvasCache permiten inyectar un DataStore para pruebas; su comportamiento de producción conserva el mismo almacenamiento por defecto. Esto evita que las pruebas dependan del singleton global de otro contexto para aislar sus archivos.

Las variantes de notas usan `grade_variant_<cuenta>_<periodo>_<carrera>`. Al arrancar se precargan las variantes válidas disponibles y su fecha; al seleccionar se utiliza la correspondiente sin esperar la red. La clave anterior por periodo sigue siendo legible si incluye la identidad correcta. La clave de notas actuales usada por workers/widgets se conserva. Se prefiere la copia con timestamp más reciente cuando hay ambas. No se cambia ninguna fórmula de calificación.

Se exige que el JSON histórico incluya periodo, carrera y cursos. Una prueba detectó que Gson asignaba UG por defecto si faltaba carrera; se corrigió el lector, sin modificar la prueba ni borrar la caché. Una copia incompleta no se utiliza bajo una identidad inventada.

La estrategia sigue siendo local primero y revalidación después, conservando datos ante error y rechazo de respuestas incompletas. La nueva política de retención solo se aplica a `navigation:`, `files:` y `topics:`. Los listados vencidos no se restauran y se purgan de forma diferida al guardar otros listados administrados; no existe un servicio de borrado continuo. No se agregan bytes de documentos, mensajes ni submissions a esta nueva caché. Las demás cachés mantienen su política previa, vinculada a la cuenta y a su eliminación explícita.

Las listas de archivos y temas muestran la fecha y la etiqueta de datos guardados. Que una lista esté disponible offline no implica que sus archivos puedan descargarse sin red, que una URL firmada siga vigente ni que los permisos de Canvas hayan permanecido iguales. Las operaciones siguen pasando por los repositorios y autorizaciones existentes.

Los documentos descargados por el visor ya tenían eliminación en `CanvasDocumentViewModel.onCleared`; no se convirtió ese almacenamiento temporal en una biblioteca permanente. Una muerte abrupta del proceso puede dejar temporales hasta limpieza de caché del sistema o de la cuenta; no se afirma una purga garantizada por tiempo de todos los documentos previos.

## Notificaciones, historial e idempotencia

La identidad del evento incluye generación de snapshot: un cambio 12→14 y el posterior 14→12 no comparten identidad, aunque se haya visto el valor 12 anteriormente. El mismo evento reintentado antes de confirmar el snapshot sí comparte clave. Se utiliza esa clave tanto para la huella de entrega como para el historial, incluso si Android bloquea el aviso.

El historial añade un campo opcional `event_key`; el JSON antiguo sin ese campo se conserva. Las marcas de leído son atómicas y un reintento no devuelve un evento leído a no leído. El cambio es aditivo y no requiere reset de datos. Las huellas viejas se conservan; no se intenta deduplicar retroactivamente avisos antiguos usando solo su texto, pues podría fusionar cambios legítimos.

Esto asegura inserción idempotente por clave en el almacenamiento local, no entrega exactamente una vez entre Android, DataStore y SharedPreferences. Las transacciones de estos sistemas siguen siendo independientes. Los IDs de sistema permanecen estables durante un reintento y `setOnlyAlertOnce` reduce alertas repetidas. El espacio de IDs de notificación es finito; no se presenta como garantía matemática de ausencia de colisiones.

Se verificó en código que el periódico de asistencia conserva 15 minutos, flex de 5, red requerida y backoff exponencial. El respaldo de notas conserva 15 minutos y consulta sin iniciar un servicio desde segundo plano. La contención solicita retry. Arranque/actualización reconcilian los trabajos y la cuenta/sesión se comprueba antes de publicar. No se añadieron categorías, servicios, permisos, backend ni FCM.

## Diagnóstico y observabilidad

Ajustes conserva último intento, último éxito, resultado, categoría de error, tiempo transcurrido, conectividad y contadores de avisos emitidos/bloqueados. Añade eventos detectados por identidad estable y estado de canales (sin crear, habilitado o bloqueado).

WorkInfo se consulta fuera del hilo principal para cuatro nombres de trabajo: periódico/manual de notas y periódico/manual de asistencia. Se muestra ID técnico abreviado, estado, intento, constraint de red, próxima elegibilidad y stopReason numérico. No se muestran inputs, tokens, contraseñas, cuenta ni contenido académico. No se registra la cadena completa de excepciones en este diagnóstico.

Se distinguen omisiones por sesión no recordada, credenciales ausentes, plazo adaptativo o mutex ocupado. Un Result.success del scheduler no se usa como prueba de una consulta exitosa: el último éxito real se guarda por separado.

La próxima elegibilidad es la primera oportunidad, no una fecha prometida. WorkInfo no ofrece un historial completo de todas las decisiones del OEM ni la hora exacta futura. El motivo de parada puede no estar disponible; por eso no se traduce automáticamente a «Doze». No se usa la API interna de JobScheduler ni se afirma una observación integral de sus colas.

## Receptor de actualizaciones y revisión de advertencias

El registro anterior ya usaba exportación explícita en Android 13+, pero Lint rechazaba la rama anterior. Se unificó mediante AndroidX, compatible con minSdk 24. Se usa RECEIVER_EXPORTED porque DownloadManager es un emisor externo; utilizar NOT_EXPORTED sin verificar su UID podría perder entregas de componentes del sistema.

Además del action y downloadId se consulta el estado real del DownloadManager. La instalación sigue exigiendo paquete, versión posterior y firma compatible. No se suprimió el error ni se añadió baseline. La comprobación estática no se convierte en prueba de que antes existiera una explotación reproducida; la recepción/instalación real necesita Android.

Se revisaron las advertencias existentes por categoría. ApplySharedPref no se corrigió indiscriminadamente: algunos commits son necesarios para confirmar dedupe y snapshot. SetJavaScriptEnabled corresponde al navegador Canvas: requiere JavaScript y ya desactiva acceso a archivos/contenido local, contenido mixto, ventanas automáticas y no expone un bridge JavaScript. Se conserva, sin declarar que eso elimine todos los riesgos del contenido web. Las sugerencias de dependencias, iconos, TOML y APIs obsoletas no justificaron una refactorización masiva.

## Pruebas

Suite JVM ampliada: **249 pruebas; 249 aprobadas; 0 fallos, 0 errores, 0 omitidas**. Incluye las integraciones existentes con MockWebServer. Son pruebas simuladas; no acreditan una sesión real Banner/Canvas.

Se añadieron 12 pruebas JVM: 10 en StabilizationTest para historial, transición de valores, compatibilidad JSON, identidad académica y retención; 2 en CanvasViewModelTest para refresco automático con datos y caché vacía completa. La primera ejecución encontró el defecto de carrera por defecto; la misma suite quedó aprobada después de corregir el lector.

AcademicCachePersistenceTest conserva sus tres pruebas y añade dos: actualización concurrente del historial con reintentos e integración de los listados nuevos con límite de tamaño. Sus DataStores se crean en archivos de prueba explícitos y las credenciales son sintéticas. **Las cinco se compilaron, no se ejecutaron.**

Las fases write/read para persistencia entre procesos siguen disponibles. Deben ejecutar únicamente el método `diskSnapshotAndTimestampSurviveRecreationOrProcessRestart`, para que una prueba de limpieza selectiva no altere el fixture entre fases. Ejecutar toda la clase dos veces no sustituye esa comprobación.

Las regresiones JVM disponibles cubren login simulado, autenticación, parsers Banner, HTTP/paginación Canvas, discusiones/mensajería, lectura y transporte de documentos, notas, asistencia, horario, calculadoras, agenda, ownership de recordatorios, limpieza de sesión, widgets/contratos de release y otras reglas. No se declara una aprobación funcional de todas esas pantallas en un dispositivo.

## Rendimiento: medición disponible y preparada

No se midieron tiempos reales de inicio, primera imagen, renderizado de caché, memoria, navegación o actualización de una cuenta universitaria. No existe un baseline Android comparable en el entorno; no se informa ningún porcentaje de mejora.

Se añadieron registros `UPAO_LoadTiming` con `phase=cache_restored` y `phase=network_round`, duración monotónica y cantidad de errores. No incluyen datos del estudiante. El primer marcador termina al publicar el estado restaurado, no al dibujar un frame. Tampoco cuenta automáticamente todas las solicitudes HTTP ni atribuye su duración a Banner/Canvas por separado.

Protocolo preparado: comparar arranque con/sin caché, online/offline, red lenta y reapertura; capturar `adb shell am start -W` y los marcadores; medir frames/memoria con herramientas Android; contrastar WorkInfo y Logcat con los cambios disponibles en origen. Repetir recientes, bloqueo, reposo, ahorro, Wi-Fi/datos, reconexión y reinicio. Separar forzar detención desde Ajustes de retirar de recientes. Usar cuentas sintéticas en pruebas que modifican almacenamiento.

## Validación, APK y Git

Comando de validación:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
```

Resultado final del comando: **BUILD SUCCESSFUL**. Lint: **0 errores, 145 advertencias y 2 sugerencias**. Se corrigió tanto el error previo del receptor como una indentación introducida durante esta fase; no hay baseline ni supresiones nuevas. Las advertencias restantes incluyen sugerencias de KTX/commits, dependencias, iconos y el navegador JavaScript revisado.

APK generado: **28,934,154 bytes (27.59 MiB)**. SHA-256: `eef31480da5522efc25ab7c074721e8bf768deac8c4d1737c361d3b4a9cd1c67`.

Resultados y registros de esta fase: `app/build/reports/stabilization/`; pruebas: `app/build/reports/tests/testDebugUnitTest/index.html`; lint: `app/build/reports/lint-results-debug.html` y XML. El archivo `results.json` de esta fase registra los totales finales, tamaño y SHA-256 del APK.

APK debug: `D:\Proyectos\UPAO PROC\UPAO S\app\build\outputs\apk\debug\app-debug.apk`. No se generó una distribución de producción ni se usaron credenciales de firma privadas. No se instaló ni desinstaló la aplicación de ningún usuario. Un APK debug no necesariamente puede actualizar una instalación de producción con otra firma.

Archivos modificados en esta fase: AppUpdater.kt; ApiCache.kt; GradesCache.kt (inyección de almacén de prueba); CanvasCache.kt; NotificacionModels.kt; UpaoRepository.kt; DashboardViewModel.kt; CanvasResourceViewModel.kt; CanvasCourseDetailView.kt; AsistenciaWorker.kt; GradeBackupWorker.kt; GradeSyncCheck.kt; SyncDiagnostics.kt; SettingsScreen.kt; CanvasViewModelTest.kt; AcademicCachePersistenceTest.kt. Nuevos: GradeVariant.kt, NotificationHistory.kt, CanvasResourceRetention.kt, StabilizationTest.kt y este informe. El detalle con rutas está en `changed-this-phase.json`.

La rama sigue siendo main. No se borraron archivos anteriores, no se restauró HEAD y no se hicieron commits ni pushes. `git diff --check` no detectó errores de espacios en los cambios.

## Limitaciones y pendientes reales

- **Corregido y probado en JVM:** idempotencia del historial, transición de notas, esquema/identidad de variantes, retención y estados de refresco del ViewModel.
- **Corregido, pendiente Android:** restauración visual de variantes/listados, concurrencia en DataStore real, receptor de actualización y diagnóstico WorkInfo/canales.
- **Parcial:** persistencia de todos los recursos Canvas (no se añade persistencia universal de mensajes, conversaciones individuales, submissions o documentos); observabilidad histórica completa del scheduler; pruebas de actualización real del APK.
- **Limitación externa:** periodicidad mínima y ejecución no exacta; restricciones de batería/Doze/OEM; force-stop; límites del servicio dataSync existente; publicación tardía en origen sin timestamp verificable.
- **Pendiente:** cinco pruebas instrumentadas, proceso write/read, actualización sobre APK firmado compatible, recepción real en segundo plano y latencia. La puntualidad real no está declarada resuelta.

Fuentes oficiales: [receivers y exportación](https://developer.android.com/develop/background-work/background-tasks/broadcasts), [WorkInfo y elegibilidad](https://developer.android.com/reference/androidx/work/WorkInfo), [PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest).
