package com.example.upaos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer
import java.util.Locale

data class AcademicIconEntry(
    val id: String,
    val icon: ImageVector,
    val phrases: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

val CourseIconBadgeLight = Color(0xFF1A6DFF)
val CourseIconBadgeDark = Color(0xFF2563EB)

/**
 * Catálogo amplio de iconos vectoriales académicos (250+ opciones distintas)
 * para asignar un icono representativo y consistente por asignatura en Cursos y Asistencia.
 */
val ACADEMIC_COURSE_ICON_CATALOG: List<AcademicIconEntry> = listOf(
    // --- Ingeniería de Software, Programación y Sistemas ---
    AcademicIconEntry("code", Icons.Rounded.Code, listOf("programacion", "ingenieria de software", "desarrollo de software", "lenguaje de programacion"), listOf("programacion", "algoritmo", "algoritmos", "codigo", "coding", "compilador", "software")),
    AcademicIconEntry("terminal", Icons.Rounded.Terminal, listOf("sistemas operativos", "sistema operativo", "linea de comandos"), listOf("linux", "unix", "shell", "terminal", "compiladores", "operativos")),
    AcademicIconEntry("developer_mode", Icons.Rounded.DeveloperMode, listOf("desarrollo movil", "aplicaciones moviles", "programacion movil"), listOf("movil", "mobile", "android", "ios", "flutter", "kotlin")),
    AcademicIconEntry("web", Icons.Rounded.Web, listOf("desarrollo web", "aplicaciones web", "ingenieria web"), listOf("web", "frontend", "fullstack", "sitio", "portal")),
    AcademicIconEntry("html", Icons.Rounded.Html, listOf("diseno web", "maquetacion web"), listOf("html", "semantica")),
    AcademicIconEntry("css", Icons.Rounded.Css, listOf("hojas de estilo", "interfaces web"), listOf("css", "estilos", "stylesheet")),
    AcademicIconEntry("javascript", Icons.Rounded.Javascript, listOf("programacion javascript", "frameworks web"), listOf("javascript", "typescript", "node", "react", "angular")),
    AcademicIconEntry("php", Icons.Rounded.Php, listOf("programacion servidor", "backend web"), listOf("php", "laravel", "backend")),
    AcademicIconEntry("data_object", Icons.Rounded.DataObject, listOf("estructura de datos", "estructuras de datos", "programacion orientada a objetos"), listOf("poo", "objetos", "estructuras", "json", "clases")),
    AcademicIconEntry("data_array", Icons.Rounded.DataArray, listOf("algebra lineal", "matrices y vectores", "arreglos"), listOf("matriz", "matrices", "vector", "vectores", "lineal")),
    AcademicIconEntry("integration", Icons.Rounded.IntegrationInstructions, listOf("agile development", "metodologias agiles", "desarrollo agil", "integracion continua"), listOf("agile", "agil", "scrum", "devops", "sprint")),
    AcademicIconEntry("bug_report", Icons.Rounded.BugReport, listOf("calidad de software", "pruebas de software", "aseguramiento de la calidad"), listOf("testing", "pruebas", "qa", "depuracion")),
    AcademicIconEntry("build", Icons.Rounded.Build, listOf("construccion de software", "mantenimiento de software"), listOf("herramientas", "reparacion", "taller mecanico")),
    AcademicIconEntry("handyman", Icons.Rounded.Handyman, listOf("soporte tecnico", "ensamblaje de computadoras"), listOf("ensamblaje", "mecanica aplicada")),
    AcademicIconEntry("dev_board", Icons.Rounded.DeveloperBoard, listOf("arquitectura de computadoras", "sistemas digitales", "circuitos digitales"), listOf("microcontrolador", "microcontroladores", "arduino", "fpga", "digitales", "placa")),
    AcademicIconEntry("memory", Icons.Rounded.Memory, listOf("machine learning", "aprendizaje automatico", "deep learning", "redes neuronales"), listOf("machine", "learning", "neuronal", "neuronales", "gpu", "cpu", "microprocesador")),
    AcademicIconEntry("psychology", Icons.Rounded.Psychology, listOf("inteligencia artificial", "psicologia general", "psicologia clinica"), listOf("ia", "inteligencia", "artificial", "psicologia", "cognitiva", "mente")),
    AcademicIconEntry("smart_toy", Icons.Rounded.SmartToy, listOf("robotica", "agentes inteligentes", "sistemas expertos"), listOf("robotica", "robot", "agentes", "bot", "autonomos")),
    AcademicIconEntry("precision_mfg", Icons.Rounded.PrecisionManufacturing, listOf("automatizacion industrial", "manufactura integrada", "control industrial"), listOf("manufactura", "cnc", "actuadores")),
    AcademicIconEntry("settings_suggest", Icons.Rounded.SettingsSuggest, listOf("automatizacion inteligente", "automatizacion de procesos", "ingenieria de procesos"), listOf("automatizacion", "rpa", "procesos", "optimizacion")),
    AcademicIconEntry("account_tree", Icons.Rounded.AccountTree, listOf("arquitectura de sistemas", "arquitectura empresarial", "arquitectura de software"), listOf("sistemas", "jerarquia", "patrones", "uml")),
    AcademicIconEntry("schema", Icons.Rounded.Schema, listOf("modelado de procesos", "ingenieria de requisitos", "analisis y diseno"), listOf("requisitos", "modelado", "bpm", "diagramas", "entidad")),
    AcademicIconEntry("alt_route", Icons.Rounded.AltRoute, listOf("teoria de decisiones", "matematica discreta", "logica matematica"), listOf("discreta", "logica", "grafos", "decisiones", "rutas")),
    AcademicIconEntry("webhook", Icons.Rounded.Webhook, listOf("servicios web", "arquitectura orientada a servicios", "microservicios"), listOf("microservicios", "soa", "webhook", "eventos")),
    AcademicIconEntry("api", Icons.Rounded.Api, listOf("integracion de sistemas", "interoperabilidad", "interfaces de programacion"), listOf("api", "apis", "rest", "graphql", "integracion")),
    AcademicIconEntry("devices", Icons.Rounded.Devices, listOf("interaccion humano computador", "experiencia de usuario", "usabilidad"), listOf("ux", "ui", "usabilidad", "multidispositivo", "interaccion")),
    AcademicIconEntry("phone_android", Icons.Rounded.PhoneAndroid, listOf("computacion movil", "tecnologias moviles"), listOf("celular", "smartphone", "moviles")),
    AcademicIconEntry("laptop_mac", Icons.Rounded.LaptopMac, listOf("introduccion a la computacion", "ofimatica", "alfabetizacion digital"), listOf("computacion", "informatica", "ofimatica", "computo")),
    AcademicIconEntry("desktop_windows", Icons.Rounded.DesktopWindows, listOf("computacion grafica", "graficacion por computadora"), listOf("graficacion", "render", "ventana", "escritorio")),
    AcademicIconEntry("keyboard", Icons.Rounded.Keyboard, listOf("digitacion", "procesamiento de texto"), listOf("teclado", "digitacion")),

    // --- Datos, Cloud, Redes, Seguridad, IoT, Visión ---
    AcademicIconEntry("storage", Icons.Rounded.Storage, listOf("base de datos", "bases de datos", "administracion de bases de datos"), listOf("sql", "nosql", "oracle", "postgres", "almacenamiento", "tablas")),
    AcademicIconEntry("dns", Icons.Rounded.Dns, listOf("gobierno de datos", "gestion de datos", "infraestructura de ti"), listOf("gobierno", "servidores", "datacenter", "infraestructura")),
    AcademicIconEntry("cloud", Icons.Rounded.Cloud, listOf("computacion en la nube", "cloud computing", "arquitectura cloud"), listOf("cloud", "nube", "aws", "azure", "gcp")),
    AcademicIconEntry("cloud_queue", Icons.Rounded.CloudQueue, listOf("virtualizacion", "servicios en la nube"), listOf("virtualizacion", "contenedores", "docker", "kubernetes")),
    AcademicIconEntry("cloud_sync", Icons.Rounded.CloudSync, listOf("sistemas distribuidos", "computacion distribuida"), listOf("distribuidos", "distribuida", "replicacion", "sincronizacion")),
    AcademicIconEntry("hub", Icons.Rounded.Hub, listOf("internet de las cosas", "ecosistemas digitales"), listOf("iot", "cosas", "nodos", "conectividad", "malla")),
    AcademicIconEntry("router", Icons.Rounded.Router, listOf("redes y comunicaciones", "redes de computadoras", "conmutacion y enrutamiento"), listOf("redes", "red", "enrutamiento", "tcp", "ip", "cisco", "router")),
    AcademicIconEntry("lan", Icons.Rounded.Lan, listOf("cableado estructurado", "topologia de redes"), listOf("lan", "wan", "topologia", "ethernet")),
    AcademicIconEntry("wifi", Icons.Rounded.Wifi, listOf("redes inalambricas", "comunicaciones inalambricas"), listOf("inalambrica", "inalambricas", "wifi")),
    AcademicIconEntry("cell_tower", Icons.Rounded.CellTower, listOf("telecomunicaciones", "antenas y propagacion", "sistemas de comunicacion"), listOf("telecomunicaciones", "antenas", "propagacion", "telefonia", "5g")),
    AcademicIconEntry("sensors", Icons.Rounded.Sensors, listOf("instrumentacion electronica", "sensores y actuadores", "telemetria"), listOf("sensores", "sensor", "telemetria", "transductores")),
    AcademicIconEntry("security", Icons.Rounded.Security, listOf("seguridad informatica", "ciberseguridad", "seguridad de la informacion"), listOf("ciberseguridad", "seguridad", "firewall", "vulnerabilidades")),
    AcademicIconEntry("shield", Icons.Rounded.Shield, listOf("defensa nacional", "seguridad ciudadana", "proteccion de datos"), listOf("defensa", "proteccion", "resiliencia", "blindaje")),
    AcademicIconEntry("lock", Icons.Rounded.Lock, listOf("criptografia", "privacidad y seguridad"), listOf("criptografia", "cifrado", "hash", "ssl", "tls")),
    AcademicIconEntry("vpn_key", Icons.Rounded.VpnKey, listOf("gestion de identidades", "control de acceso"), listOf("identidad", "accesos", "autenticacion", "claves")),
    AcademicIconEntry("admin_panel", Icons.Rounded.AdminPanelSettings, listOf("auditoria de sistemas", "auditoria informatica", "gobierno de ti"), listOf("auditoria", "cobit", "itil", "compliance")),
    AcademicIconEntry("policy", Icons.Rounded.Policy, listOf("etica informatica", "marco legal digital", "politicas publicas"), listOf("politicas", "normativa", "iso", "regimen")),
    AcademicIconEntry("radar", Icons.Rounded.Radar, listOf("percepcion computacional", "sistemas de deteccion", "vision artificial"), listOf("percepcion", "deteccion", "lidar", "radar")),
    AcademicIconEntry("visibility", Icons.Rounded.Visibility, listOf("vision por computadora", "procesamiento de imagenes"), listOf("vision", "imagenes", "opencv")),
    AcademicIconEntry("focus_strong", Icons.Rounded.CenterFocusStrong, listOf("reconocimiento de patrones", "biometria"), listOf("patrones", "biometria", "segmentacion", "enfoque")),
    AcademicIconEntry("camera_alt", Icons.Rounded.CameraAlt, listOf("fotografia digital", "lenguaje fotografico"), listOf("fotografia", "iluminacion", "retrato")),
    AcademicIconEntry("graphic_eq", Icons.Rounded.GraphicEq, listOf("procesamiento de senales", "procesamiento digital de senales", "acustica"), listOf("senales", "audio", "frecuencia", "espectro", "acustica")),
    AcademicIconEntry("waves", Icons.Rounded.Waves, listOf("ondas y electromagnetismo", "campos electromagneticos"), listOf("ondas", "electromagnetismo", "maxwell", "resonancia")),
    AcademicIconEntry("satellite_alt", Icons.Rounded.SatelliteAlt, listOf("teledeteccion", "sistemas satelitales", "geomatica"), listOf("satelite", "satelital", "teledeteccion", "gps", "gnss")),
    AcademicIconEntry("qr_code", Icons.Rounded.QrCode2, listOf("trazabilidad", "codificacion digital"), listOf("trazabilidad", "codificacion", "etiquetado")),
    AcademicIconEntry("nfc", Icons.Rounded.Nfc, listOf("sistemas embebidos", "identificacion por radiofrecuencia"), listOf("rfid", "nfc", "embebidos", "embebido")),
    AcademicIconEntry("speed", Icons.Rounded.Speed, listOf("ingenieria de rendimiento", "optimizacion de sistemas"), listOf("rendimiento", "velocidad", "latencia", "metricas")),
    AcademicIconEntry("sd_storage", Icons.Rounded.SdStorage, listOf("arquitectura de almacenamiento", "memoria y almacenamiento"), listOf("disco", "particiones")),

    // --- Matemáticas, Estadística, Física, Química e Ingenierías ---
    AcademicIconEntry("calculate", Icons.Rounded.Calculate, listOf("matematica basica", "calculo diferencial", "calculo integral"), listOf("calculo", "matematica", "matematicas", "aritmetica", "ecuaciones")),
    AcademicIconEntry("functions", Icons.Rounded.Functions, listOf("analisis matematico", "ecuaciones diferenciales", "variable compleja"), listOf("funciones", "derivadas", "integrales", "diferenciales", "fourier")),
    AcademicIconEntry("percent", Icons.Rounded.Percent, listOf("matematica financiera", "estadistica descriptiva"), listOf("porcentaje", "tasas", "interes", "descuento")),
    AcademicIconEntry("bar_chart", Icons.Rounded.BarChart, listOf("estadistica aplicada", "estadistica general", "probabilidad y estadistica"), listOf("estadistica", "estadistico", "histograma", "frecuencias")),
    AcademicIconEntry("show_chart", Icons.Rounded.ShowChart, listOf("econometria", "series de tiempo", "analisis economico"), listOf("econometria", "series", "regresion", "tendencia")),
    AcademicIconEntry("stacked_line", Icons.Rounded.StackedLineChart, listOf("analisis multivariado", "pronosticos"), listOf("multivariado", "pronostico", "varianza")),
    AcademicIconEntry("pie_chart", Icons.Rounded.PieChart, listOf("teoria de probabilidades", "muestreo estadistico"), listOf("probabilidad", "probabilidades", "muestreo", "distribucion")),
    AcademicIconEntry("bubble_chart", Icons.Rounded.BubbleChart, listOf("mineria de datos", "ciencia de datos", "big data"), listOf("mineria", "clustering", "analytics", "big")),
    AcademicIconEntry("scatter_plot", Icons.Rounded.ScatterPlot, listOf("modelos predictivos", "analisis numerico", "metodos numericos"), listOf("numericos", "interpolacion", "correlacion", "dispersion")),
    AcademicIconEntry("query_stats", Icons.Rounded.QueryStats, listOf("inteligencia de negocios", "business intelligence", "analitica de datos"), listOf("analitica", "kpi", "dashboard")),
    AcademicIconEntry("insights", Icons.Rounded.Insights, listOf("investigacion de mercados", "ciencia de decisiones"), listOf("insights", "hallazgos", "inferencia")),
    AcademicIconEntry("timeline", Icons.Rounded.Timeline, listOf("investigacion de operaciones", "programacion lineal", "historia contemporanea"), listOf("operaciones", "simplex", "pert", "cpm", "cronologia")),
    AcademicIconEntry("science", Icons.Rounded.Science, listOf("quimica general", "quimica organica", "quimica inorganica", "quimica analitica"), listOf("quimica", "laboratorio", "reacciones", "reactivos", "molecular")),
    AcademicIconEntry("biotech", Icons.Rounded.Biotech, listOf("biologia celular", "biotecnologia", "genetica humana", "biologia molecular"), listOf("biologia", "biotecnologia", "genetica", "adn", "celular", "microscopio")),
    AcademicIconEntry("bolt", Icons.Rounded.Bolt, listOf("fisica general", "fisica ii", "electricidad y magnetismo"), listOf("fisica", "electricidad", "voltaje", "corriente", "carga")),
    AcademicIconEntry("electric_bolt", Icons.Rounded.ElectricBolt, listOf("circuitos electricos", "analisis de circuitos", "maquinas electricas"), listOf("circuitos", "electrotecnia", "potencia", "impedancia")),
    AcademicIconEntry("electrical_services", Icons.Rounded.ElectricalServices, listOf("instalaciones electricas", "subestaciones electricas"), listOf("instalaciones", "tableros")),
    AcademicIconEntry("battery_charging", Icons.Rounded.BatteryChargingFull, listOf("almacenamiento de energia", "sistemas de potencia"), listOf("bateria", "energia", "eficiencia", "energetica")),
    AcademicIconEntry("solar_power", Icons.Rounded.SolarPower, listOf("energias renovables", "energia solar"), listOf("fotovoltaica", "renovable", "renovables")),
    AcademicIconEntry("wind_power", Icons.Rounded.WindPower, listOf("energia eolica", "mecanica de fluidos", "aerodinamica"), listOf("eolica", "fluidos", "flujo", "turbinas", "viento")),
    AcademicIconEntry("thermostat", Icons.Rounded.Thermostat, listOf("termodinamica", "transferencia de calor"), listOf("termodinamica", "calor", "temperatura", "entalpia", "entropia")),
    AcademicIconEntry("ac_unit", Icons.Rounded.AcUnit, listOf("refrigeracion y aire acondicionado", "criogenia"), listOf("refrigeracion", "climatizacion", "termico")),
    AcademicIconEntry("whatshot", Icons.Rounded.Whatshot, listOf("procesos termicos", "seguridad contra incendios"), listOf("combustion", "hornos", "caldera", "fuego")),
    AcademicIconEntry("water_drop", Icons.Rounded.WaterDrop, listOf("hidraulica", "hidrologia", "ingenieria sanitaria", "recursos hidricos"), listOf("hidraulica", "hidrologia", "agua", "sanitaria", "riego", "drenaje")),
    AcademicIconEntry("engineering", Icons.Rounded.Engineering, listOf("introduccion a la ingenieria", "ingenieria de metodos", "estatica y dinamica"), listOf("ingenieria", "estatica", "dinamica", "mecanica", "resistencia")),
    AcademicIconEntry("factory", Icons.Rounded.Factory, listOf("ingenieria industrial", "gestion de la produccion", "procesos industriales"), listOf("produccion", "planta", "fabrica", "lean")),
    AcademicIconEntry("construction", Icons.Rounded.Construction, listOf("ingenieria civil", "tecnologia del concreto", "gestion de la construccion"), listOf("civil", "construccion", "obra", "obras", "concreto", "pavimentos")),
    AcademicIconEntry("square_foot", Icons.Rounded.SquareFoot, listOf("geometria descriptiva", "dibujo tecnico", "dibujo de ingenieria"), listOf("geometria", "trigonometria", "cad", "plano", "escala")),
    AcademicIconEntry("straighten", Icons.Rounded.Straighten, listOf("topografia", "metrologia", "normalizacion"), listOf("topografia", "metrologia", "medicion", "nivelacion")),
    AcademicIconEntry("cable", Icons.Rounded.Cable, listOf("electronica analogica", "electronica de potencia"), listOf("electronica", "semiconductores", "diodos", "transistores")),

    // --- Medicina, Salud, Enfermería, Estomatología, Farmacia, Nutrición ---
    AcademicIconEntry("medical_services", Icons.Rounded.MedicalServices, listOf("semiologia medica", "clinica medica", "medicina interna", "primeros auxilios"), listOf("medicina", "clinica", "semiologia", "diagnostico", "medico")),
    AcademicIconEntry("local_hospital", Icons.Rounded.LocalHospital, listOf("salud publica", "gestion hospitalaria", "internado medico"), listOf("hospital", "hospitalaria", "clinico", "internado", "epidemiologia")),
    AcademicIconEntry("healing", Icons.Rounded.Healing, listOf("tecnica operatoria", "cirugia general", "enfermeria quirurgica"), listOf("cirugia", "quirurgica", "curacion", "sutura", "terapia")),
    AcademicIconEntry("monitor_heart", Icons.Rounded.MonitorHeart, listOf("fisiologia humana", "fisiopatologia", "cardiologia"), listOf("fisiologia", "fisiopatologia", "cardiologia", "cardiovascular", "signos")),
    AcademicIconEntry("favorite", Icons.Rounded.Favorite, listOf("anatomia humana", "cuidado integral", "salud integral"), listOf("anatomia", "corazon", "organismo", "vital")),
    AcademicIconEntry("medication", Icons.Rounded.Medication, listOf("farmacologia", "terapeutica medica", "farmacia clinica"), listOf("farmacologia", "farmacia", "farmaco", "medicamentos", "dosis")),
    AcademicIconEntry("vaccines", Icons.Rounded.Vaccines, listOf("inmunologia", "salud preventiva", "inmunizaciones"), listOf("inmunologia", "vacunas", "vacunacion", "antigenos")),
    AcademicIconEntry("bloodtype", Icons.Rounded.Bloodtype, listOf("hematologia", "analisis clinicos", "bioquimica clinica"), listOf("hematologia", "sangre", "hemograma", "bioquimica")),
    AcademicIconEntry("coronavirus", Icons.Rounded.Coronavirus, listOf("microbiologia", "parasitologia", "virologia", "patologia"), listOf("microbiologia", "parasitologia", "virologia", "patologia", "bacterias")),
    AcademicIconEntry("sanitizer", Icons.Rounded.Sanitizer, listOf("bioseguridad", "control de infecciones"), listOf("bioseguridad", "esterilizacion", "asepsia", "higiene")),
    AcademicIconEntry("clean_hands", Icons.Rounded.CleanHands, listOf("fundamentos de enfermeria", "atencion primaria", "cuidados de enfermeria"), listOf("enfermeria", "cuidado", "cuidados", "paciente")),
    AcademicIconEntry("personal_injury", Icons.Rounded.PersonalInjury, listOf("traumatologia", "salud ocupacional", "medicina legal"), listOf("traumatologia", "ortopedia", "lesiones", "emergencias")),
    AcademicIconEntry("accessibility_new", Icons.Rounded.AccessibilityNew, listOf("kinesiologia", "ergonomia", "terapia fisica y rehabilitacion"), listOf("ergonomia", "rehabilitacion", "postura", "locomotor")),
    AcademicIconEntry("directions_run", Icons.Rounded.DirectionsRun, listOf("educacion fisica", "deporte universitario", "cultura fisica"), listOf("deporte", "deportes", "atletismo")),
    AcademicIconEntry("fitness_center", Icons.Rounded.FitnessCenter, listOf("biomecanica", "entrenamiento deportivo"), listOf("biomecanica", "muscular", "fuerza", "gimnasia")),
    AcademicIconEntry("self_improvement", Icons.Rounded.SelfImprovement, listOf("salud mental", "desarrollo personal", "psicoterapia"), listOf("mental", "psicoterapia", "bienestar", "mindfulness")),
    AcademicIconEntry("mood", Icons.Rounded.Mood, listOf("habilidades blandas", "inteligencia emocional", "psicologia del desarrollo"), listOf("emocional", "blandas", "actitud", "motivacion")),
    AcademicIconEntry("hearing", Icons.Rounded.Hearing, listOf("otorrinolaringologia", "fonoaudiologia", "audiologia"), listOf("audicion", "oido", "fonoaudiologia")),
    AcademicIconEntry("remove_red_eye", Icons.Rounded.RemoveRedEye, listOf("oftalmologia", "histologia", "embriologia"), listOf("histologia", "embriologia", "oftalmologia")),
    AcademicIconEntry("restaurant", Icons.Rounded.Restaurant, listOf("nutricion humana", "bromatologia", "dietetica"), listOf("nutricion", "bromatologia", "dietetica", "alimentos", "alimentaria")),
    AcademicIconEntry("local_dining", Icons.Rounded.LocalDining, listOf("gastronomia", "tecnologia alimentaria", "industrias alimentarias"), listOf("alimentarias", "gastronomia", "inocuidad", "nutrientes")),
    AcademicIconEntry("spa", Icons.Rounded.Spa, listOf("medicina complementaria", "botanica farmaceutica"), listOf("botanica", "fitoterapia", "relajacion")),
    AcademicIconEntry("child_care", Icons.Rounded.ChildCare, listOf("pediatria", "neonatologia", "educacion inicial"), listOf("pediatria", "neonatologia", "infantil", "ninos", "inicial")),
    AcademicIconEntry("elderly", Icons.Rounded.Elderly, listOf("geriatria", "gerontologia"), listOf("geriatria", "gerontologia", "adulto mayor")),
    AcademicIconEntry("pregnant_woman", Icons.Rounded.PregnantWoman, listOf("ginecologia y obstetricia", "salud materno infantil"), listOf("obstetricia", "ginecologia", "materno", "perinatal")),
    AcademicIconEntry("pets", Icons.Rounded.Pets, listOf("medicina veterinaria", "zootecnia", "clinica veterinaria"), listOf("veterinaria", "zootecnia", "animal", "animales", "fauna")),
    AcademicIconEntry("medical_info", Icons.Rounded.MedicalInformation, listOf("estomatologia", "odontologia", "historia clinica"), listOf("estomatologia", "odontologia", "dental", "endodoncia", "ortodoncia")),
    AcademicIconEntry("health_safety", Icons.Rounded.HealthAndSafety, listOf("seguridad y salud en el trabajo", "prevencion de riesgos"), listOf("ssoma", "prevencion", "riesgos", "salubridad")),

    // --- Administración, Economía, Finanzas, Contabilidad, Marketing, Logística ---
    AcademicIconEntry("business", Icons.Rounded.Business, listOf("administracion general", "administracion de empresas", "gestion estrategica"), listOf("administracion", "empresa", "empresas", "organizacional", "corporativa")),
    AcademicIconEntry("business_center", Icons.Rounded.BusinessCenter, listOf("gerencia estrategica", "direccion empresarial", "consultoria empresarial"), listOf("gerencia", "direccion", "consultoria", "ejecutiva")),
    AcademicIconEntry("work", Icons.Rounded.Work, listOf("practicas preprofesionales", "insercion laboral", "empleabilidad"), listOf("practicas", "preprofesionales", "laboral", "pasantia")),
    AcademicIconEntry("badge", Icons.Rounded.Badge, listOf("gestion del talento humano", "recursos humanos"), listOf("talento", "rrhh", "personal", "seleccion", "planillas")),
    AcademicIconEntry("attach_money", Icons.Rounded.AttachMoney, listOf("costos y presupuestos", "ingenieria economica"), listOf("costos", "presupuesto", "presupuestos", "moneda", "precio")),
    AcademicIconEntry("monetization_on", Icons.Rounded.MonetizationOn, listOf("microeconomia", "macroeconomia", "economia general"), listOf("economia", "microeconomia", "macroeconomia", "mercado", "pib")),
    AcademicIconEntry("wallet", Icons.Rounded.AccountBalanceWallet, listOf("contabilidad general", "contabilidad financiera", "contabilidad de costos"), listOf("contabilidad", "contable", "tesoreria", "banca", "balances")),
    AcademicIconEntry("savings", Icons.Rounded.Savings, listOf("finanzas corporativas", "mercado de capitales", "evaluacion de proyectos"), listOf("finanzas", "financiera", "inversion", "rentabilidad", "capital")),
    AcademicIconEntry("credit_card", Icons.Rounded.CreditCard, listOf("gestion bancaria", "fintech", "medios de pago"), listOf("bancaria", "fintech", "credito", "financiero")),
    AcademicIconEntry("receipt_long", Icons.Rounded.ReceiptLong, listOf("derecho tributario", "tributacion", "auditoria tributaria"), listOf("tributacion", "tributario", "impuestos", "sunat", "niif")),
    AcademicIconEntry("request_quote", Icons.Rounded.RequestQuote, listOf("formulacion y evaluacion de proyectos", "analisis financiero"), listOf("valuacion", "cotizacion", "flujo de caja")),
    AcademicIconEntry("price_check", Icons.Rounded.PriceCheck, listOf("control interno", "control de gestion"), listOf("peritaje", "presupuestal")),
    AcademicIconEntry("currency_exchange", Icons.Rounded.CurrencyExchange, listOf("comercio internacional", "negocios internacionales", "comercio exterior"), listOf("internacionales", "exportacion", "importacion", "aduanas", "divisas")),
    AcademicIconEntry("trending_up", Icons.Rounded.TrendingUp, listOf("gestion de ventas", "crecimiento empresarial", "bolsa de valores"), listOf("ventas", "bolsa", "acciones", "crecimiento")),
    AcademicIconEntry("storefront", Icons.Rounded.Storefront, listOf("gestion comercial", "retail", "emprendimiento"), listOf("comercial", "retail", "tienda", "franquicia")),
    AcademicIconEntry("shopping_cart", Icons.Rounded.ShoppingCart, listOf("comercio electronico", "e-commerce", "comportamiento del consumidor"), listOf("ecommerce", "consumidor", "compra", "compras")),
    AcademicIconEntry("shopping_bag", Icons.Rounded.ShoppingBag, listOf("trade marketing", "merchandasing"), listOf("merchandising", "producto", "marca")),
    AcademicIconEntry("campaign", Icons.Rounded.Campaign, listOf("fundamentos de marketing", "marketing estrategico", "publicidad"), listOf("marketing", "mercadotecnia", "publicidad", "campana", "propaganda")),
    AcademicIconEntry("ads_click", Icons.Rounded.AdsClick, listOf("marketing digital", "estrategia digital"), listOf("seo", "sem", "trafico", "conversion")),
    AcademicIconEntry("sell", Icons.Rounded.Sell, listOf("politica de precios", "etiquetado y marca"), listOf("branding", "oferta", "demanda")),
    AcademicIconEntry("local_shipping", Icons.Rounded.LocalShipping, listOf("gestion logistica", "cadena de suministro", "transporte y distribucion"), listOf("logistica", "suministro", "transporte", "flota", "envios")),
    AcademicIconEntry("inventory2", Icons.Rounded.Inventory2, listOf("gestion de inventarios", "control de operaciones", "almacenes"), listOf("inventario", "inventarios", "almacen", "stock", "abastecimiento")),
    AcademicIconEntry("warehouse", Icons.Rounded.Warehouse, listOf("centros de distribucion", "supply chain"), listOf("distribucion", "bodega", "logistico")),
    AcademicIconEntry("support_agent", Icons.Rounded.SupportAgent, listOf("customer development", "gestion de clientes", "servicio al cliente"), listOf("customer", "cliente", "clientes", "crm", "atencion")),
    AcademicIconEntry("handshake", Icons.Rounded.Handshake, listOf("negociacion empresarial", "resolucion de conflictos", "conciliacion"), listOf("negociacion", "acuerdos", "conciliacion", "alianzas", "socios")),
    AcademicIconEntry("groups", Icons.Rounded.Groups, listOf("liderazgo y trabajo en equipo", "dinamica de grupos", "sociologia"), listOf("equipo", "equipos", "liderazgo", "sociologia", "comunidad")),
    AcademicIconEntry("diversity3", Icons.Rounded.Diversity3, listOf("comportamiento organizacional", "responsabilidad social", "cultura organizacional"), listOf("cultura", "sociedad", "diversidad", "inclusion")),
    AcademicIconEntry("rocket_launch", Icons.Rounded.RocketLaunch, listOf("innovacion y emprendimiento", "creacion de empresas", "design thinking"), listOf("emprendimiento", "innovacion", "startup", "startups", "emprendedor")),

    // --- Derecho, Ciencias Políticas, Humanidades, Idiomas e Historia ---
    AcademicIconEntry("gavel", Icons.Rounded.Gavel, listOf("derecho penal", "derecho procesal", "teoria del delito", "litigacion oral"), listOf("penal", "procesal", "juicio", "litigacion", "delito", "juez")),
    AcademicIconEntry("balance", Icons.Rounded.Balance, listOf("derecho civil", "introduccion al derecho", "derechos humanos", "filosofia del derecho"), listOf("derecho", "justicia", "obligaciones", "reales", "sucesiones")),
    AcademicIconEntry("account_balance", Icons.Rounded.AccountBalance, listOf("derecho constitucional", "derecho administrativo", "derecho publico", "instituciones politicas"), listOf("constitucional", "constitucion", "estado", "publico", "republica")),
    AcademicIconEntry("assured_workload", Icons.Rounded.AssuredWorkload, listOf("gestion publica", "contrataciones del estado", "derecho municipal"), listOf("publica", "municipal", "regional", "estatal")),
    AcademicIconEntry("fact_check", Icons.Rounded.FactCheck, listOf("derecho registral y notarial", "auditoria de calidad"), listOf("registral", "notarial", "checklist")),
    AcademicIconEntry("rule", Icons.Rounded.Rule, listOf("derecho laboral", "legislacion laboral", "reglamento"), listOf("legislacion", "normas", "reglas", "estatuto")),
    AcademicIconEntry("description", Icons.Rounded.Description, listOf("acto juridico", "contratos", "titulos valores", "redaccion tecnica"), listOf("contratos", "juridico", "actos", "documentacion")),
    AcademicIconEntry("article", Icons.Rounded.Article, listOf("periodismo escrito", "generos periodisticos", "redaccion periodistica"), listOf("articulo", "prensa", "cronica", "ensayo", "editorial")),
    AcademicIconEntry("history_edu", Icons.Rounded.HistoryEdu, listOf("historia del peru", "derecho romano", "historia universal"), listOf("historia", "romano", "antigua")),
    AcademicIconEntry("museum", Icons.Rounded.Museum, listOf("historia del arte", "arqueologia", "museologia", "cultura peruana"), listOf("arqueologia", "museo", "prehispanica")),
    AcademicIconEntry("auto_stories", Icons.Rounded.AutoStories, listOf("literatura", "lectura critica", "comprension lectora"), listOf("literatura", "lectura", "obra", "novela", "poesia")),
    AcademicIconEntry("menu_book", Icons.Rounded.MenuBook, listOf("metodologia del aprendizaje", "filosofia", "teoria general"), listOf("filosofia", "epistemologia", "doctrina", "fundamentos")),
    AcademicIconEntry("library_books", Icons.Rounded.LibraryBooks, listOf("bibliografia", "gestion documental", "seminario de tesis"), listOf("documental", "archivo", "fuentes", "enciclopedia")),
    AcademicIconEntry("book", Icons.Rounded.Book, listOf("lenguaje", "lengua espanola", "lenguaje y comunicacion"), listOf("lengua", "lenguaje", "espanol", "castellano")),
    AcademicIconEntry("edit_note", Icons.Rounded.EditNote, listOf("redaccion academica", "comunicacion escrita", "taller de redaccion"), listOf("redaccion", "escritura", "sintaxis", "composicion")),
    AcademicIconEntry("spellcheck", Icons.Rounded.Spellcheck, listOf("ortografia", "linguistica", "gramatica"), listOf("ortografia", "linguistica", "gramatica", "fonetica")),
    AcademicIconEntry("translate", Icons.Rounded.Translate, listOf("ingles i", "ingles ii", "ingles tecnico", "idiomas"), listOf("ingles", "english", "idioma", "idiomas", "frances", "traduccion")),
    AcademicIconEntry("language", Icons.Rounded.Language, listOf("relaciones internacionales", "globalizacion", "derecho internacional"), listOf("internacional", "globalizacion", "diplomacia", "geopolitica")),
    AcademicIconEntry("public", Icons.Rounded.Public, listOf("realidad nacional", "geografia", "ciencias sociales"), listOf("nacional", "mundial", "geografia", "mundo", "global")),
    AcademicIconEntry("forum", Icons.Rounded.Forum, listOf("comunicacion efectiva", "argumentacion juridica", "debate"), listOf("comunicacion", "argumentacion", "debate", "dialogo")),
    AcademicIconEntry("record_voice_over", Icons.Rounded.RecordVoiceOver, listOf("oratoria", "expresion oral", "locucion"), listOf("oratoria", "oral", "voz", "discurso", "retorica")),
    AcademicIconEntry("question_answer", Icons.Rounded.QuestionAnswer, listOf("entrevista psicologica", "comunicacion interpersonal"), listOf("entrevista", "interpersonal")),
    AcademicIconEntry("volunteer_activism", Icons.Rounded.VolunteerActivism, listOf("etica y deontologia", "etica profesional", "ciudadania y derechos"), listOf("etica", "deontologia", "valores", "ciudadania", "voluntariado")),
    AcademicIconEntry("how_to_vote", Icons.Rounded.HowToVote, listOf("ciencia politica", "derecho electoral", "democracia"), listOf("politica", "electoral", "democracia", "gobernabilidad")),

    // --- Arquitectura, Diseño, Urbanismo, Artes y Comunicación Audiovisual ---
    AcademicIconEntry("architecture", Icons.Rounded.Architecture, listOf("taller de diseno arquitectonico", "teoria de la arquitectura", "historia de la arquitectura"), listOf("arquitectura", "arquitectonico", "arquitectonica", "compas")),
    AcademicIconEntry("apartment", Icons.Rounded.Apartment, listOf("edificaciones", "diseno de edificios", "construccion sostenible"), listOf("edificios", "edificaciones", "inmuebles", "fachada")),
    AcademicIconEntry("location_city", Icons.Rounded.LocationCity, listOf("urbanismo", "planificacion urbana", "diseno urbano", "ordenamiento territorial"), listOf("urbanismo", "urbano", "urbana", "ciudad", "metropoli")),
    AcademicIconEntry("home_work", Icons.Rounded.HomeWork, listOf("arquitectura habitacional", "vivienda social", "diseno interior"), listOf("vivienda", "interiores", "residencial", "habitacional")),
    AcademicIconEntry("foundation", Icons.Rounded.Foundation, listOf("mecanica de suelos", "cimentaciones", "estructuras"), listOf("suelos", "geotecnia", "cimentacion", "zapata", "puentes")),
    AcademicIconEntry("domain", Icons.Rounded.Domain, listOf("proyectos arquitectonicos", "patrimonio monumental"), listOf("patrimonio", "monumental", "inmobiliaria")),
    AcademicIconEntry("design_services", Icons.Rounded.DesignServices, listOf("diseno grafico", "diseno industrial", "diseno de producto"), listOf("diseno", "boceto", "prototipo", "maquetacion")),
    AcademicIconEntry("palette", Icons.Rounded.Palette, listOf("teoria del color", "taller de pintura", "artes plasticas"), listOf("color", "pintura", "arte", "artes", "cromatica")),
    AcademicIconEntry("brush", Icons.Rounded.Brush, listOf("expresion artistica", "ilustracion", "taller de arte"), listOf("pincel", "ilustracion", "acuarela", "escultura")),
    AcademicIconEntry("draw", Icons.Rounded.Draw, listOf("dibujo arquitectonico", "dibujo artistico", "croquis"), listOf("dibujo", "trazo", "perspectiva", "croquis")),
    AcademicIconEntry("color_lens", Icons.Rounded.ColorLens, listOf("diseno multimedia", "comunicacion visual", "estetica"), listOf("visual", "estetica", "multimedia", "creatividad")),
    AcademicIconEntry("style", Icons.Rounded.Style, listOf("identidad visual", "semiotica", "diseno editorial"), listOf("semiotica", "editorial", "tipografia", "moda")),
    AcademicIconEntry("view_in_ar", Icons.Rounded.ViewInAr, listOf("modelado 3d", "realidad virtual", "realidad aumentada", "metodologia bim"), listOf("3d", "bim", "revit", "autocad", "realidad", "espacial")),
    AcademicIconEntry("layers", Icons.Rounded.Layers, listOf("sistemas de informacion geografica", "arquitectura por capas"), listOf("sig", "gis", "capas", "estratos")),
    AcademicIconEntry("grid_on", Icons.Rounded.GridOn, listOf("diagramacion", "reticula compositiva", "calculo matricial"), listOf("reticula", "cuadricula", "composicion")),
    AcademicIconEntry("crop", Icons.Rounded.Crop, listOf("edicion digital", "postproduccion"), listOf("edicion", "encuadre", "formato")),
    AcademicIconEntry("photo_library", Icons.Rounded.PhotoLibrary, listOf("portafolio fotografico", "archivo visual"), listOf("portafolio", "galeria", "album")),
    AcademicIconEntry("videocam", Icons.Rounded.Videocam, listOf("produccion audiovisual", "comunicacion audiovisual", "taller de video"), listOf("audiovisual", "video", "camara", "rodaje", "filmacion")),
    AcademicIconEntry("movie", Icons.Rounded.Movie, listOf("lenguaje cinematografico", "guion audiovisual", "cine"), listOf("cine", "guion", "montaje", "cortometraje")),
    AcademicIconEntry("theater_comedy", Icons.Rounded.TheaterComedy, listOf("artes escenicas", "teatro", "expresion corporal"), listOf("teatro", "escenicas", "dramaturgia", "actuacion")),
    AcademicIconEntry("music_note", Icons.Rounded.MusicNote, listOf("apreciacion musical", "musica", "cultura artistica"), listOf("musica", "musical", "ritmo")),
    AcademicIconEntry("headphones", Icons.Rounded.Headphones, listOf("produccion sonora", "diseno sonoro", "podcast"), listOf("sonido", "sonoro", "podcast", "mezcla")),
    AcademicIconEntry("mic", Icons.Rounded.Mic, listOf("periodismo radial", "produccion radial", "radio"), listOf("radio", "radial", "microfono")),
    AcademicIconEntry("newspaper", Icons.Rounded.Newspaper, listOf("periodismo de investigacion", "periodismo digital", "opinion publica"), listOf("periodismo", "noticias", "noticiero", "medios")),
    AcademicIconEntry("live_tv", Icons.Rounded.LiveTv, listOf("produccion televisiva", "television"), listOf("television", "transmision", "streaming")),

    // --- Ecología, Medio Ambiente, Agronomía, Transporte, Investigación y Gestión ---
    AcademicIconEntry("forest", Icons.Rounded.Forest, listOf("ecologia y medio ambiente", "manejo forestal", "conservacion ambiental"), listOf("ecologia", "forestal", "bosques", "biodiversidad", "flora")),
    AcademicIconEntry("park", Icons.Rounded.Park, listOf("paisajismo", "areas verdes", "impacto ambiental"), listOf("ambiental", "paisajismo", "parques", "ambiente", "sostenibilidad")),
    AcademicIconEntry("energy_leaf", Icons.Rounded.EnergySavingsLeaf, listOf("gestion ambiental", "desarrollo sostenible", "ecoeficiencia"), listOf("sostenible", "ecoeficiencia", "ecologico", "verde")),
    AcademicIconEntry("compost", Icons.Rounded.Compost, listOf("gestion de residuos", "tratamiento de residuos", "economia circular"), listOf("residuos", "circular", "organico", "compostaje")),
    AcademicIconEntry("recycling", Icons.Rounded.Recycling, listOf("reciclaje industrial", "reutilizacion de materiales"), listOf("reciclaje", "reutilizacion", "saneamiento")),
    AcademicIconEntry("agriculture", Icons.Rounded.Agriculture, listOf("ingenieria agronomica", "agroindustria", "cultivos"), listOf("agronomia", "agroindustria", "agricultura", "cultivo", "cosecha")),
    AcademicIconEntry("yard", Icons.Rounded.Yard, listOf("botanica general", "fisiologia vegetal", "horticultura"), listOf("vegetal", "plantas", "semillas")),
    AcademicIconEntry("terrain", Icons.Rounded.Terrain, listOf("geologia", "geomorfologia", "mecanica de rocas"), listOf("geologia", "rocas", "mineria", "relieve", "caminos")),
    AcademicIconEntry("landscape", Icons.Rounded.Landscape, listOf("ecoturismo", "geografia fisica", "analisis territorial"), listOf("paisaje", "territorio", "territorial")),
    AcademicIconEntry("explore", Icons.Rounded.Explore, listOf("geodesia", "orientacion profesional"), listOf("geodesia", "brujula", "exploracion")),
    AcademicIconEntry("map", Icons.Rounded.Map, listOf("cartografia", "planificacion regional"), listOf("cartografia", "mapa", "mapas")),
    AcademicIconEntry("directions_car", Icons.Rounded.DirectionsCar, listOf("ingenieria de transito", "seguridad vial"), listOf("transito", "vial", "vehicular", "carreteras")),
    AcademicIconEntry("flight", Icons.Rounded.Flight, listOf("comercio aereo", "aerodinamica aplicada"), listOf("aereo", "aviacion", "drones", "aeronautica")),
    AcademicIconEntry("anchor", Icons.Rounded.Anchor, listOf("derecho maritimo", "ingenieria portuaria", "logistica portuaria"), listOf("maritimo", "portuaria", "puertos", "naviera")),
    AcademicIconEntry("wb_sunny", Icons.Rounded.WbSunny, listOf("climatologia", "meteorologia"), listOf("clima", "meteorologia", "radiacion")),
    AcademicIconEntry("air", Icons.Rounded.Air, listOf("contaminacion atmosferica", "calidad del aire"), listOf("aire", "atmosfera", "ventilacion", "neumatica")),
    AcademicIconEntry("assignment", Icons.Rounded.Assignment, listOf("gestion de proyectos", "formulacion de proyectos", "taller de proyectos"), listOf("proyecto", "proyectos", "pmi", "pmbok", "entregables")),
    AcademicIconEntry("task_alt", Icons.Rounded.TaskAlt, listOf("gestion de la calidad", "control de calidad", "mejora continua"), listOf("mejora", "kaizen", "six sigma")),
    AcademicIconEntry("checklist", Icons.Rounded.Checklist, listOf("auditoria operativa", "planificacion estrategica"), listOf("planificacion", "estrategica", "metas")),
    AcademicIconEntry("folder_shared", Icons.Rounded.FolderShared, listOf("gestion del conocimiento", "trabajo colaborativo"), listOf("colaborativo")),
    AcademicIconEntry("workspaces", Icons.Rounded.Workspaces, listOf("seminario de investigacion", "taller multidisciplinario"), listOf("seminario", "talleres", "sinergia")),
    AcademicIconEntry("lightbulb", Icons.Rounded.Lightbulb, listOf("creatividad e innovacion", "pensamiento critico"), listOf("creatividad", "pensamiento", "critico", "ideas")),
    AcademicIconEntry("emoji_objects", Icons.Rounded.EmojiObjects, listOf("solucion de problemas", "heuristica"), listOf("innovar", "soluciones", "inventiva")),
    AcademicIconEntry("school", Icons.Rounded.School, listOf("introduccion a la vida universitaria", "pedagogia", "didactica"), listOf("universitaria", "pedagogia", "didactica", "docencia", "educacion")),
    AcademicIconEntry("workspace_premium", Icons.Rounded.WorkspacePremium, listOf("tesis i", "tesis ii", "proyecto de tesis", "trabajo de investigacion"), listOf("tesis", "titulacion", "bachiller", "grado", "excelencia")),
    AcademicIconEntry("manage_search", Icons.Rounded.ManageSearch, listOf("metodologia de la investigacion", "investigacion cientifica"), listOf("investigacion", "metodologia", "cientifica", "hipotesis")),
    AcademicIconEntry("find_in_page", Icons.Rounded.FindInPage, listOf("analisis de textos", "auditoria documental"), listOf("revision", "busqueda")),
    AcademicIconEntry("assessment", Icons.Rounded.Assessment, listOf("evaluacion educativa", "evaluacion del desempeno"), listOf("evaluacion", "desempeno", "valoracion")),
    AcademicIconEntry("table_chart", Icons.Rounded.TableChart, listOf("contabilidad intermedia", "analisis tabular"), listOf("estados financieros", "excel")),
    AcademicIconEntry("pivot_table", Icons.Rounded.PivotTableChart, listOf("modelamiento de datos", "cubos olap"), listOf("olap", "pivote", "dimensiones")),
    AcademicIconEntry("auto_graph", Icons.Rounded.AutoGraph, listOf("simulacion de sistemas", "dinamica de sistemas"), listOf("simulacion", "tendencias")),
    AcademicIconEntry("extension", Icons.Rounded.Extension, listOf("desarrollo de videojuegos", "gamificacion"), listOf("videojuegos", "juegos", "gamificacion", "modulos")),
    AcademicIconEntry("widgets", Icons.Rounded.Widgets, listOf("componentes de software", "sistemas modulares"), listOf("componentes", "modulares")),
    AcademicIconEntry("tune", Icons.Rounded.Tune, listOf("teoria de control", "control automatico", "calibracion"), listOf("calibracion", "pid", "sintonizacion", "parametros")),
    AcademicIconEntry("quiz", Icons.Rounded.Quiz, listOf("logica proposicional", "razonamiento verbal", "razonamiento matematico"), listOf("razonamiento", "examen", "preguntas")),
    AcademicIconEntry("calendar_month", Icons.Rounded.CalendarMonth, listOf("programacion de obras", "cronograma de proyectos"), listOf("cronograma", "agenda", "planificador")),
    AcademicIconEntry("schedule", Icons.Rounded.Schedule, listOf("estudio del trabajo", "tiempos y movimientos"), listOf("tiempos", "movimientos", "productividad")),
    AcademicIconEntry("co_present", Icons.Rounded.CoPresent, listOf("sustentacion", "ponencia", "comunicacion corporativa"), listOf("exposicion", "sustentacion", "presentacion")),
    AcademicIconEntry("cast_education", Icons.Rounded.CastForEducation, listOf("educacion a distancia", "tecnologia educativa", "e-learning"), listOf("elearning", "educativa", "pedagogico")),
    AcademicIconEntry("print", Icons.Rounded.Print, listOf("artes graficas", "impresion y preprensa"), listOf("preprensa", "impresion")),
    AcademicIconEntry("scanner", Icons.Rounded.Scanner, listOf("digitalizacion documental", "imagenes medicas"), listOf("escaner", "tomografia", "radiologia", "digitalizacion")),

    // --- Especialidades complementarias (para superar 250 opciones únicas verificadas) ---
    AcademicIconEntry("fingerprint", Icons.Rounded.Fingerprint, listOf("criminalistica", "medicina forense", "autenticacion biometrica"), listOf("forense", "criminalistica", "criminologia", "dactiloscopia")),
    AcademicIconEntry("verified_user", Icons.Rounded.VerifiedUser, listOf("seguridad del paciente", "derecho informatico"), listOf("certificacion", "garantia", "acreditacion", "firma digital")),
    AcademicIconEntry("local_police", Icons.Rounded.LocalPolice, listOf("derecho policial", "seguridad estrategica"), listOf("criminologico", "policial", "orden")),
    AcademicIconEntry("emergency", Icons.Rounded.Emergency, listOf("emergencias medicas", "medicina de urgencias"), listOf("urgencias", "triaje", "rescate")),
    AcademicIconEntry("touch_app", Icons.Rounded.TouchApp, listOf("interfaces tactiles", "interactividad"), listOf("interfaz", "tactil", "interactivo")),
    AcademicIconEntry("usb", Icons.Rounded.Usb, listOf("arquitectura de perifericos", "hardware de computador"), listOf("hardware", "perifericos", "puertos", "buses")),
    AcademicIconEntry("sd_card", Icons.Rounded.SdCard, listOf("sistemas de archivos", "arquitectura de memoria"), listOf("archivos", "flash")),
    AcademicIconEntry("bookmarks", Icons.Rounded.Bookmarks, listOf("estudio de casos", "lectura selecta"), listOf("lecturas", "casos", "referencias")),
    AcademicIconEntry("category", Icons.Rounded.Category, listOf("clasificacion", "taxonomia", "morfologia"), listOf("taxonomia", "categorias", "morfologia")),
    AcademicIconEntry("track_changes", Icons.Rounded.TrackChanges, listOf("direccion por objetivos", "seguimiento de metas"), listOf("objetivos", "metas")),
    AcademicIconEntry("compass_calibration", Icons.Rounded.CompassCalibration, listOf("orientacion espacial", "calibracion magnetica"), listOf("orientacion", "magnetico")),
    AcademicIconEntry("navigation", Icons.Rounded.Navigation, listOf("navegacion maritima", "sistemas de guiado"), listOf("guiado", "trayectoria", "rumbo")),
    AcademicIconEntry("directions_bus", Icons.Rounded.DirectionsBus, listOf("transporte urbano", "movilidad sostenible"), listOf("movilidad", "transporte publico")),
    AcademicIconEntry("thunderstorm", Icons.Rounded.Thunderstorm, listOf("hidrometeorologia", "cambio climatico"), listOf("climatico", "precipitacion", "glaciares")),
    AcademicIconEntry("water", Icons.Rounded.Water, listOf("oceanografia", "ingenieria costera"), listOf("marino", "costera", "cuencas")),
    AcademicIconEntry("psychology_alt", Icons.Rounded.PsychologyAlt, listOf("psicologia cognitiva", "neuropsicologia", "filosofia de la mente"), listOf("neuropsicologia", "dilemas")),
    AcademicIconEntry("grain", Icons.Rounded.Grain, listOf("ciencia de materiales", "nanotecnologia", "estructura de materiales"), listOf("materiales", "nanotecnologia", "polimeros", "metalurgia", "cristalografia")),
    AcademicIconEntry("blur_on", Icons.Rounded.BlurOn, listOf("mecanica cuantica", "optica moderna"), listOf("cuantica", "fotonica", "particulas", "atomica")),
    AcademicIconEntry("mediation", Icons.Rounded.Mediation, listOf("mediacion y arbitraje", "arbitraje comercial"), listOf("arbitraje", "mediacion", "peritacion")),
    AcademicIconEntry("gpp_good", Icons.Rounded.GppGood, listOf("seguridad de redes", "gestion de riesgos ti"), listOf("iso27001")),
    AcademicIconEntry("verified", Icons.Rounded.Verified, listOf("gestion de acreditacion", "auditoria de procesos"), listOf("acreditado", "iso9001", "validacion")),
    AcademicIconEntry("donut_large", Icons.Rounded.DonutLarge, listOf("estadistica bayesiana", "analisis porcentual"), listOf("bayesiana", "proporciones")),
    AcademicIconEntry("candlestick_chart", Icons.Rounded.CandlestickChart, listOf("mercado financiero", "finanzas bursatiles", "derivados financieros"), listOf("bursatil", "trading", "derivados")),
    AcademicIconEntry("waterfall_chart", Icons.Rounded.WaterfallChart, listOf("flujos financieros", "analisis de variaciones"), listOf("variaciones", "cascada")),
    AcademicIconEntry("currency_bitcoin", Icons.Rounded.CurrencyBitcoin, listOf("blockchain", "criptoactivos", "economia digital"), listOf("blockchain", "criptomonedas")),
    AcademicIconEntry("payments", Icons.Rounded.Payments, listOf("gestion de tesoreria", "remuneraciones", "finanzas publicas"), listOf("pagos", "remuneraciones", "nomina")),
    AcademicIconEntry("point_of_sale", Icons.Rounded.PointOfSale, listOf("facturacion electronica", "gestion de punto de venta"), listOf("facturacion", "boletas")),
    AcademicIconEntry("real_estate", Icons.Rounded.RealEstateAgent, listOf("tasacion inmobiliaria", "derecho inmobiliario"), listOf("inmobiliario", "tasacion", "bienes raices")),
    AcademicIconEntry("corporate_fare", Icons.Rounded.CorporateFare, listOf("arquitectura institucional", "derecho societario"), listOf("societario", "corporativo", "institucion")),
    AcademicIconEntry("villa", Icons.Rounded.Villa, listOf("arquitectura vernacula", "diseno residencial"), listOf("vernacula", "arquitectural")),
    AcademicIconEntry("add_road", Icons.Rounded.AddRoad, listOf("ingenieria de carreteras", "infraestructura vial"), listOf("carretera", "pavimento", "vias")),
    AcademicIconEntry("plumbing", Icons.Rounded.Plumbing, listOf("instalaciones sanitarias", "redes de tuberia"), listOf("tuberias", "desague")),
    AcademicIconEntry("carpenter", Icons.Rounded.Carpenter, listOf("tecnologia de la madera", "diseno de mobiliario"), listOf("madera", "mobiliario", "carpinteria")),
    AcademicIconEntry("hardware", Icons.Rounded.Hardware, listOf("tecnologia mecanica", "procesos de taller"), listOf("herramental", "forja")),
    AcademicIconEntry("nights_stay", Icons.Rounded.NightsStay, listOf("astronomia", "astrofisica"), listOf("astronomia", "astrofisica", "cosmo")),
    AcademicIconEntry("podcasts", Icons.Rounded.Podcasts, listOf("comunicacion digital", "narrativa transmedia"), listOf("transmedia", "radiofonico")),
    AcademicIconEntry("interpreter_mode", Icons.Rounded.InterpreterMode, listOf("interpretacion simultanea", "traduccion especializada"), listOf("interprete", "bilingue", "idiomatico")),
    AcademicIconEntry("subtitles", Icons.Rounded.Subtitles, listOf("guionismo", "subtitulaje audiovisual"), listOf("subtitulos", "narracion")),
    AcademicIconEntry("animation", Icons.Rounded.Animation, listOf("animacion digital", "animacion 2d y 3d"), listOf("animacion", "motion")),
    AcademicIconEntry("hexagon", Icons.Rounded.Hexagon, listOf("quimica estructural", "estereoquimica"), listOf("benceno", "aromaticos", "estructural")),
    AcademicIconEntry("colorize", Icons.Rounded.Colorize, listOf("tecnologia farmaceutica", "quimica farmaceutica"), listOf("jarabes", "galenica", "formulacion")),
    AcademicIconEntry("local_pharmacy", Icons.Rounded.LocalPharmacy, listOf("atencion farmaceutica", "toxicologia"), listOf("toxicologia", "dispensacion", "botica")),
    AcademicIconEntry("flood", Icons.Rounded.Flood, listOf("gestion de riesgos y desastres", "drenaje pluvial"), listOf("desastres", "inundaciones", "pluvial")),
    AcademicIconEntry("tsunami", Icons.Rounded.Tsunami, listOf("ingenieria sismorresistente", "dinamica estructural"), listOf("sismica", "sismo", "sismorresistente")),
    AcademicIconEntry("volcano", Icons.Rounded.Volcano, listOf("geodinamica", "mineralogia"), listOf("mineralogia", "petrologia", "geodinamica")),
    AcademicIconEntry("grass", Icons.Rounded.Grass, listOf("pasturas y forrajes", "edafologia"), listOf("edafologia", "forrajes", "fertilidad")),
    AcademicIconEntry("pest_control", Icons.Rounded.PestControl, listOf("entomologia", "sanidad vegetal"), listOf("entomologia", "plagas", "fitosanidad")),
    AcademicIconEntry("dataset", Icons.Rounded.Dataset, listOf("ingenieria de datos", "etl y bodegas de datos"), listOf("dataset", "datawarehouse", "etl")),
    AcademicIconEntry("view_kanban", Icons.Rounded.ViewKanban, listOf("gestion agil de proyectos", "flujos kanban"), listOf("kanban", "backlog")),
    AcademicIconEntry("difference", Icons.Rounded.Difference, listOf("control de versiones", "analisis comparativo"), listOf("git", "versiones")),
    AcademicIconEntry("polyline", Icons.Rounded.Polyline, listOf("grafos y arboles", "geometria computacional"), listOf("polilinea", "vectorial")),
    AcademicIconEntry("troubleshoot", Icons.Rounded.Troubleshoot, listOf("diagnostico de sistemas", "mantenimiento predictivo"), listOf("predictivo", "fallas")),
    AcademicIconEntry("oil_barrel", Icons.Rounded.OilBarrel, listOf("ingenieria petroquimica", "petroleo y gas"), listOf("petroquimica", "petroleo", "refinacion")),
    AcademicIconEntry("sports_soccer", Icons.Rounded.SportsSoccer, listOf("deportes colectivos", "futbol"), listOf("futbol", "deportiva")),
    AcademicIconEntry("travel_explore", Icons.Rounded.TravelExplore, listOf("turismo internacional", "geografia turistica"), listOf("turismo", "destinos")),
    AcademicIconEntry("luggage", Icons.Rounded.Luggage, listOf("administracion hotelera y turismo", "gestion hotelera"), listOf("hoteleria", "hoteles", "hospitalidad")),
    AcademicIconEntry("model_training", Icons.Rounded.ModelTraining, listOf("ciencia de modelos", "entrenamiento de modelos"), listOf("entrenamiento", "hiperparametros")),
    AcademicIconEntry("token", Icons.Rounded.Token, listOf("procesamiento de lenguaje natural", "criptoeconomia"), listOf("nlp", "tokens", "linguistica computacional")),
    AcademicIconEntry("copyright", Icons.Rounded.Copyright, listOf("derechos de autor", "propiedad artistica", "propiedad intelectual"), listOf("autor", "licencias", "plagio", "patentes")),
    AcademicIconEntry("vpn_lock", Icons.Rounded.VpnLock, listOf("redes privadas virtuales", "ciberdefensa"), listOf("vpn", "ciberdefensa")),
    AcademicIconEntry("plagiarism", Icons.Rounded.Plagiarism, listOf("integridad academica", "revision bibliografica"), listOf("bibliografica", "citacion", "apa"))
)
    .distinctBy { it.icon }

private fun normalizeCourseName(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun resolveCourseSubjectIcon(courseName: String): ImageVector {
    val normalized = normalizeCourseName(courseName)
    if (normalized.isBlank()) return Icons.Rounded.MenuBook

    val tokens = normalized.split(" ").filter { it.isNotBlank() }.toSet()
    var bestEntry: AcademicIconEntry? = null
    var bestScore = 0

    for (entry in ACADEMIC_COURSE_ICON_CATALOG) {
        var score = 0
        for (phrase in entry.phrases) {
            if (normalized.contains(phrase)) {
                score = maxOf(score, 100 + phrase.length)
            }
        }
        for (kw in entry.keywords) {
            when {
                tokens.contains(kw) -> score = maxOf(score, 50 + kw.length)
                kw.length >= 4 && tokens.any { it.startsWith(kw) } -> score = maxOf(score, 28 + kw.length)
                kw.length >= 5 && normalized.contains(kw) -> score = maxOf(score, 16 + kw.length)
            }
        }
        if (score > bestScore) {
            bestScore = score
            bestEntry = entry
        }
    }

    if (bestEntry != null && bestScore > 0) {
        return bestEntry.icon
    }

    // Fallback determinístico basado únicamente en el nombre normalizado del curso
    // para garantizar consistencia entre Cursos y Asistencia sin aleatoriedad.
    var hash = 0x811c9dc5.toInt()
    for (ch in normalized) {
        hash = hash xor ch.code
        hash *= 0x01000193
    }
    val positiveIndex = (hash and 0x7fffffff) % ACADEMIC_COURSE_ICON_CATALOG.size
    return ACADEMIC_COURSE_ICON_CATALOG[positiveIndex].icon
}
