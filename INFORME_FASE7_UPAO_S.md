# UPAO S · Fase 7 · Tareas en Hoy

## Arquitectura y navegación

Antes, la quinta posición alternaba entre el explorador de Canvas y Pagos. Hoy usaba una consulta acoplada a la actualización académica y mostraba las entregas del día. La quinta posición ahora abre Pagos directamente. Hoy conserva sus novedades y muestra una vista previa de tres tareas, un contador y un listado completo dentro de la app. Los detalles con enlaces HTTPS válidos se abren en el sitio institucional de Canvas. El explorador anterior quedó sin entrada de navegación.

Ajustes → Integraciones ofrece la configuración de Canvas. Se reutilizan la validación contra Canvas, el almacenamiento cifrado por cuenta y el cliente Retrofit. El formulario empieza vacío y enmascara el token introducido.

## Fuente y cobertura

- `GET /api/v1/courses?enrollment_state=active&enrollment_type=student` enumera cursos con matrícula estudiantil activa.
- `GET /api/v1/courses/{id}/assignments` enumera actividades con `include[]=submission` y `override_assignment_dates=true`. Se utiliza el `due_at` efectivo de la respuesta, incluido el valor nulo.
- `GET /api/v1/courses/{id}/assignments/{id}/submissions/self` completa la comprobación cuando falta el envío propio.
- Se siguen los enlaces `Link: rel=next` hasta agotar cursos y actividades, con validación de origen y detección de ciclos. No se aplica un límite de fechas o páginas a esas dos consultas. La clave curso/actividad evita duplicados.
- El listado de tareas rápidas de Canvas queda disponible en el cliente existente. La cobertura de Hoy se basa en la enumeración de actividades de cada curso, con el envío del estudiante.

Referencias del contrato: [Cursos](https://developerdocs.instructure.com/services/canvas/resources/courses), [Actividades](https://developerdocs.instructure.com/services/canvas/resources/assignments) y [Entregas](https://developerdocs.instructure.com/services/canvas/resources/submissions).

## Clasificación

La vista excluye actividades vencidas, cerradas o no publicadas, entregas registradas, entregas pendientes de revisión y actividades excusadas. El estado `graded` por sí solo no demuestra una entrega. Se usan la fecha del envío propio y sus estados, incluidos los que Canvas informa en entregas grupales. Las actividades solo presenciales, sin envío o sin calificación requieren evidencia distinta y no se presentan como entregas pendientes. Las fechas se interpretan como instantes y se clasifican en America/Lima. Las tareas sin fecha accionables aparecen al final. La fecha de cierre se conserva en el modelo y permite distinguir el cierre de la actividad de su vencimiento.

El contador distingue resultados completos de resultados parciales. La ausencia de configuración, credenciales inválidas, errores de red y carga tienen estados propios. Una respuesta parcial vacía no produce el mensaje de que el estudiante está al día.

## Sesión y actualización

Hoy carga en primer plano, reutiliza resultados completos en memoria durante cinco minutos mientras su pantalla sigue montada y permite actualización manual. El cambio de pantalla o la pausa del ciclo de vida cancela la consulta. Antes de publicar se comprueban cuenta, generación de sesión y token. Los resultados nuevos se mantienen en memoria. El cierre borra la caché de agenda de versiones anteriores. El trabajador de recordatorios preexistente dejó de consultar Canvas en segundo plano y solo puede utilizar una instantánea local previa dentro de su ventana de vigencia. No se añadieron recordatorios nuevos.

## Archivos

- Navegación y configuración: `MainActivity.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `CanvasScreen.kt`.
- Hoy: `TodayContent.kt`, `TodayCards.kt`, `TodayTasks.kt`, `DashboardViewModel.kt`.
- Datos y sesión: `PendingTasksRepository.kt`, `CanvasApiService.kt`, `CanvasPagedApi.kt`, `CanvasModels.kt`, `ApiCache.kt`, `AccountPrivacy.kt`, `AgendaReminderWorker.kt`.
- Pruebas: `PendingTasksTest.kt`, `TodayTasksScreenTest.kt` y suite existente.

## Verificación

- `.gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` terminó con `BUILD SUCCESSFUL`. Se aprobaron **273 pruebas JVM**, incluidas cinco nuevas de tareas, sin fallos ni omitidas. Gradle completó la compilación tras usar el modo alternativo del compilador Kotlin.
- En el teléfono se aprobaron **9 pruebas instrumentadas** de tareas, Pagos y navegación de retorno. La prueba de tareas cubre apertura del listado y cambio de tema con datos sintéticos.
- Se instaló el APK debug como actualización. Con la sesión autorizada, Hoy mostró tareas y un contador completo, el listado completo abrió y la barra inferior ofreció Pagos sin Canvas. Se abrió también Ajustes → Integraciones → Canvas.
- Pagos abrió directamente. La primera consulta mostró un error de conexión y el reintento cargó el saldo real.
- APK local: `app/build/outputs/apk/debug/app-debug.apk`. Versión 1.7.9, código 25. `git diff --check` sin errores.

Quedó pendiente cotejar las tareas, sus fechas y sus estados de entrega con Canvas oficial. También queda pendiente comprobar cierre y cambio entre cuentas institucionales en el teléfono. Las pruebas locales verifican vencimientos lejanos, fecha efectiva, estados de envío y paginación con datos sintéticos.
