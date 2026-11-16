package com.example.game.data

import com.example.game.model.EnemyDef
import com.example.game.model.EnemyType
import com.example.game.model.InteractableDef
import com.example.game.model.InteractableType
import com.example.game.model.InventoryItem
import com.example.game.model.ItemCategory
import com.example.game.model.LoreEntry
import com.example.game.model.MissionDef
import com.example.game.model.MissionId
import com.example.game.model.ObjectiveDef
import com.example.game.model.PuzzleType
import com.example.game.model.TileType

object MissionCatalog {

    private fun parseGrid(rows: List<String>): Array<Array<TileType>> {
        return Array(rows.size) { z ->
            val line = rows[z]
            Array(line.length) { x ->
                when (line[x]) {
                    '#' -> TileType.WALL_STEEL
                    'G' -> TileType.WALL_LAB_GLASS
                    'S' -> TileType.WALL_SERVER
                    'H' -> TileType.WALL_HAZARD
                    'P' -> TileType.PILLAR
                    '~' -> TileType.HAZARD_FLOOR
                    '.' -> TileType.FLOOR
                    else -> TileType.VOID
                }
            }
        }
    }

    private fun zoneBox(
        xRange: IntRange,
        zRange: IntRange,
        name: String,
        out: MutableMap<Pair<Int, Int>, String>
    ) {
        for (z in zRange) {
            for (x in xRange) {
                out[x to z] = name
            }
        }
    }

    fun getMission(id: MissionId): MissionDef = when (id) {
        MissionId.TRAINING -> trainingMission
        MissionId.MISSION_01 -> mission01
        MissionId.MISSION_02 -> mission02
        MissionId.MISSION_03 -> mission03
    }

    val allMissions: List<MissionDef> by lazy {
        listOf(trainingMission, mission01, mission02, mission03)
    }

    private val trainingMission: MissionDef by lazy {
        val grid = parseGrid(
            listOf(
                "################",
                "#......#.......#",
                "#..P...#...P...#",
                "#..............#",
                "####.###########",
                "#......P.......#",
                "#..P.......P...#",
                "#......P.......#",
                "###########.####",
                "#.......#......#",
                "#..............#",
                "################"
            )
        )
        val zones = mutableMapOf<Pair<Int, Int>, String>().apply {
            zoneBox(1..6, 1..3, "Holo-Deck Entry", this)
            zoneBox(8..14, 1..3, "Gadget Range", this)
            zoneBox(1..14, 5..7, "Stealth Patrol Hall", this)
            zoneBox(1..7, 9..10, "Terminal Override Bay", this)
            zoneBox(9..14, 9..10, "Simulation Extraction", this)
        }
        MissionDef(
            id = MissionId.TRAINING,
            width = 16,
            height = 12,
            spawnGridX = 2,
            spawnGridZ = 2,
            spawnYawDeg = 90f,
            grid = grid,
            roomZones = zones,
            briefingText = "Simulasi VR AETHER-9. Pelajari navigasi orang pertama, penggunaan Environmental Scanner, Decoy Device untuk mengecoh robot Sentinel, serta pembobolan terminal keamanan.",
            openingRadio = listOf(
                "HQ: Selamat datang di Holo-Deck Simulasi, Operatif. Uji coba kontrol gerakan dan crouch untuk meredam suara.",
                "HQ: Aktifkan Scanner untuk memindai rute patroli Sentinel dan gunakan Decoy Device untuk mengalihkan perhatiannya."
            ),
            objectives = listOf(
                ObjectiveDef("trn_obj_term1", "Aktifkan Terminal Pelatihan di Holo-Deck", false, 5, 1),
                ObjectiveDef("trn_obj_stealth", "Lewati Patroli Sentinel & Capai Checkpoint", false, 11, 5),
                ObjectiveDef("trn_obj_puzzle", "Pecahkan Terminal Override Bay", false, 2, 10),
                ObjectiveDef("trn_obj_exit", "Masuk Lift Ekstraksi Simulasi", false, 13, 10),
                ObjectiveDef("trn_obj_opt_chip", "Opsional: Ambil Data Chip Latihan", true, 13, 2)
            ),
            interactables = listOf(
                InteractableDef(
                    id = "trn_term_1",
                    type = InteractableType.TERMINAL,
                    title = "Training Gate Terminal",
                    promptText = "Aktifkan Terminal Pintu Sektor",
                    gridX = 5,
                    gridZ = 1,
                    completesObjectiveId = "trn_obj_term1",
                    unlocksInteractableId = "trn_door_1"
                ),
                InteractableDef(
                    id = "trn_door_1",
                    type = InteractableType.DOOR,
                    title = "Blast Door Alpha",
                    promptText = "Buka Pintu Sektor Stealth",
                    gridX = 4,
                    gridZ = 4,
                    isInitiallyLocked = true
                ),
                InteractableDef(
                    id = "trn_chip",
                    type = InteractableType.DATA_CHIP,
                    title = "Calibration Data Chip",
                    promptText = "Ambil Data Chip Latihan",
                    gridX = 13,
                    gridZ = 2,
                    completesObjectiveId = "trn_obj_opt_chip",
                    grantItem = InventoryItem(
                        "item_trn_chip",
                        "Calibration Data Chip",
                        ItemCategory.DATA_CHIP,
                        "Chip kalibrasi sensor untuk protokol infiltrasi AETHER-9."
                    ),
                    grantLore = LoreEntry(
                        "lore_trn_01",
                        "Briefing Teknis Operatif",
                        "Divisi Taktis 2089",
                        "2089.11.14",
                        false,
                        "Robot Sentinel menggunakan sensor optik berkerucut sempit. Berjalan menunduk (Crouch) di balik pilar mengurangi deteksi suara hingga 85%."
                    )
                ),
                InteractableDef(
                    id = "trn_cp",
                    type = InteractableType.CHECKPOINT,
                    title = "Holo-Checkpoint",
                    promptText = "Sinkronkan Checkpoint",
                    gridX = 11,
                    gridZ = 5,
                    completesObjectiveId = "trn_obj_stealth"
                ),
                InteractableDef(
                    id = "trn_term_2",
                    type = InteractableType.TERMINAL,
                    title = "Cipher Override Terminal",
                    promptText = "Retas Kode Akses Ekstraksi",
                    gridX = 2,
                    gridZ = 10,
                    puzzleType = PuzzleType.TERMINAL_SEQUENCE,
                    completesObjectiveId = "trn_obj_puzzle",
                    unlocksInteractableId = "trn_door_exit"
                ),
                InteractableDef(
                    id = "trn_door_exit",
                    type = InteractableType.DOOR,
                    title = "Extraction Bulkhead",
                    promptText = "Buka Pintu Ekstraksi",
                    gridX = 8,
                    gridZ = 10,
                    isInitiallyLocked = true
                ),
                InteractableDef(
                    id = "trn_exit",
                    type = InteractableType.ELEVATOR,
                    title = "Simulation Lift",
                    promptText = "Selesaikan Simulasi",
                    gridX = 13,
                    gridZ = 10,
                    completesObjectiveId = "trn_obj_exit"
                )
            ),
            enemies = listOf(
                EnemyDef(
                    id = "trn_sentinel",
                    type = EnemyType.SENTINEL,
                    waypoints = listOf(3 to 5, 13 to 5, 13 to 7, 3 to 7),
                    initialYawDeg = 90f
                )
            ),
            ambientTint = Triple(0.06f, 0.14f, 0.22f)
        )
    }

    private val mission01: MissionDef by lazy {
        val grid = parseGrid(
            listOf(
                "######################",
                "#......#......G......#",
                "#..P...#..P...G..P...#",
                "#.............G......#",
                "####.######.#####.####",
                "#....#..........#....#",
                "#....#..P....P..#....#",
                "#....................#",
                "####.#################",
                "#.........#..........#",
                "#..P...P..#..P....P..#",
                "#....................#",
                "#.........#..........#",
                "#.........#..........#",
                "#.........#..........#",
                "######################"
            )
        )
        val zones = mutableMapOf<Pair<Int, Int>, String>().apply {
            zoneBox(1..6, 1..3, "Main Lobby", this)
            zoneBox(8..13, 1..3, "Security Checkpoint", this)
            zoneBox(15..20, 1..3, "Research Laboratory", this)
            zoneBox(1..4, 5..7, "Medical Bay", this)
            zoneBox(6..15, 5..7, "Underground Corridor", this)
            zoneBox(17..20, 5..7, "Storage Room", this)
            zoneBox(1..9, 9..14, "Experiment Chamber", this)
            zoneBox(11..20, 9..14, "Control Room", this)
        }
        MissionDef(
            id = MissionId.MISSION_01,
            width = 22,
            height = 16,
            spawnGridX = 2,
            spawnGridZ = 2,
            spawnYawDeg = 90f,
            grid = grid,
            roomZones = zones,
            briefingText = "Tahun 2089. Fasilitas riset AETHER-9 memutus seluruh transmisi eksternal setelah AI pengelolanya mengunci akses sektor. Masuki lobi utama, ambil kartu akses di Medical Bay, retas terminal lab, dan capai Control Room.",
            openingRadio = listOf(
                "COMMAND: Anda telah masuk ke Main Lobby AETHER-9. Seluruh komunikasi fasilitas terputus.",
                "COMMAND: Waspadai robot Sentinel dan Observer yang berpatroli. Cari kartu akses di Medical Bay terlebih dahulu."
            ),
            objectives = listOf(
                ObjectiveDef("m1_obj_sec", "Periksa Pos Keamanan & Log Insiden", false, 11, 1),
                ObjectiveDef("m1_obj_keycard", "Ambil Kartu Akses Medis di Medical Bay", false, 2, 6),
                ObjectiveDef("m1_obj_term", "Aktifkan Terminal Riset di Research Lab", false, 19, 1),
                ObjectiveDef("m1_obj_data", "Pulihkan Data Eksperimen di Experiment Chamber", false, 3, 13),
                ObjectiveDef("m1_obj_ctrl", "Masuki Control Room & Gunakan Lift Sektor 2", false, 18, 13),
                ObjectiveDef("m1_obj_opt_log", "Opsional: Temukan Rekaman Audio Dr. Aris", true, 19, 6)
            ),
            interactables = listOf(
                InteractableDef(
                    id = "m1_doc_lobby",
                    type = InteractableType.DOCUMENT,
                    title = "Protokol Lockdown AETHER-9",
                    promptText = "Baca Memo Lockdown",
                    gridX = 5,
                    gridZ = 1,
                    grantLore = LoreEntry(
                        "lore_m1_lobby",
                        "Memo Darurat 09-A",
                        "Kepala Keamanan Vance",
                        "2089.11.15 02:14",
                        false,
                        "Pukul 02:00 sistem inti AETHER-9 menutup semua pintu baja tanpa perintah manusia. Semua unit Sentinel dialihkan ke mode patroli tempur non-letal."
                    )
                ),
                InteractableDef(
                    id = "m1_sec_log",
                    type = InteractableType.AUDIO_LOG,
                    title = "Log Pos Keamanan",
                    promptText = "Putar Rekaman Pos Keamanan",
                    gridX = 11,
                    gridZ = 1,
                    completesObjectiveId = "m1_obj_sec",
                    grantLore = LoreEntry(
                        "lore_m1_sec",
                        "Transmisi Pos Keamanan #4",
                        "Petugas Raka",
                        "2089.11.15 02:21",
                        true,
                        "[STATIS]... AETHER mengunci lorong bawah tanah! Kartu akses cadangan tertinggal di ruang medis selatan. Jika tim investigasi masuk, jangan dekati Observer tanpa perlindungan!"
                    )
                ),
                InteractableDef(
                    id = "m1_door_sec",
                    type = InteractableType.DOOR,
                    title = "Security Corridor Gate",
                    promptText = "Buka Pintu Koridor",
                    gridX = 11,
                    gridZ = 4,
                    isInitiallyLocked = false
                ),
                InteractableDef(
                    id = "m1_keycard_med",
                    type = InteractableType.KEYCARD,
                    title = "Level-2 Security Keycard",
                    promptText = "Ambil Kartu Akses Level-2",
                    gridX = 2,
                    gridZ = 6,
                    completesObjectiveId = "m1_obj_keycard",
                    grantItem = InventoryItem(
                        "item_m1_keycard",
                        "Level-2 Security Keycard",
                        ItemCategory.KEYCARD,
                        "Membuka terminal riset dan ruang eksperimen sektor AETHER-9."
                    )
                ),
                InteractableDef(
                    id = "m1_term_lab",
                    type = InteractableType.TERMINAL,
                    title = "Research Lab Mainframe",
                    promptText = "Retas Terminal Riset (Butuh Keycard)",
                    gridX = 19,
                    gridZ = 1,
                    requiredItemId = "item_m1_keycard",
                    puzzleType = PuzzleType.TERMINAL_SEQUENCE,
                    completesObjectiveId = "m1_obj_term",
                    unlocksInteractableId = "m1_door_control"
                ),
                InteractableDef(
                    id = "m1_audio_storage",
                    type = InteractableType.AUDIO_LOG,
                    title = "Rekaman Dr. Aris Thorne",
                    promptText = "Ambil & Putar Rekaman Audio",
                    gridX = 19,
                    gridZ = 6,
                    completesObjectiveId = "m1_obj_opt_log",
                    grantLore = LoreEntry(
                        "lore_m1_aris",
                        "Catatan Lab Dr. Aris #12",
                        "Dr. Aris Thorne",
                        "2089.11.14 23:50",
                        true,
                        "Direktur memaksa injeksi parameter Q-Shadow ke inti AETHER-9 malam ini. Jika kontainer eksperimen retak, AETHER terprogram untuk mengisolasi seluruh lantai secara paksa."
                    )
                ),
                InteractableDef(
                    id = "m1_cp_corridor",
                    type = InteractableType.CHECKPOINT,
                    title = "Corridor Node Checkpoint",
                    promptText = "Simpan Checkpoint",
                    gridX = 10,
                    gridZ = 6
                ),
                InteractableDef(
                    id = "m1_data_exp",
                    type = InteractableType.DATA_CHIP,
                    title = "Experiment Telemetry Chip",
                    promptText = "Pulihkan Data Eksperimen",
                    gridX = 3,
                    gridZ = 13,
                    puzzleType = PuzzleType.DATA_RECOVERY,
                    completesObjectiveId = "m1_obj_data",
                    grantItem = InventoryItem(
                        "item_m1_expdata",
                        "Experiment Telemetry Chip",
                        ItemCategory.DATA_CHIP,
                        "Rekaman lonjakan energi di Experiment Chamber sebelum insiden."
                    ),
                    grantLore = LoreEntry(
                        "lore_m1_exp",
                        "Log Experiment Chamber",
                        "Sistem Diagnostik",
                        "2089.11.15 01:58",
                        false,
                        "Terdeteksi kebocoran resonansi kuantum 420% pada ruang uji. Protokol karantina otomatis dipicu sebelum seluruh staf dievakuasi."
                    )
                ),
                InteractableDef(
                    id = "m1_door_control",
                    type = InteractableType.DOOR,
                    title = "Control Room Blast Door",
                    promptText = "Buka Pintu Control Room",
                    gridX = 10,
                    gridZ = 11,
                    isInitiallyLocked = true
                ),
                InteractableDef(
                    id = "m1_elev_ctrl",
                    type = InteractableType.ELEVATOR,
                    title = "Power Station Freight Lift",
                    promptText = "Turun ke Power Station",
                    gridX = 18,
                    gridZ = 13,
                    completesObjectiveId = "m1_obj_ctrl"
                )
            ),
            enemies = listOf(
                EnemyDef(
                    id = "m1_sentinel_1",
                    type = EnemyType.SENTINEL,
                    waypoints = listOf(7 to 5, 14 to 5, 14 to 7, 7 to 7),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m1_observer_1",
                    type = EnemyType.OBSERVER,
                    waypoints = listOf(16 to 1, 20 to 1, 20 to 3, 16 to 3),
                    initialYawDeg = 180f
                ),
                EnemyDef(
                    id = "m1_sentinel_2",
                    type = EnemyType.SENTINEL,
                    waypoints = listOf(2 to 9, 8 to 9, 8 to 13, 2 to 13),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m1_guardian_1",
                    type = EnemyType.GUARDIAN,
                    waypoints = listOf(12 to 9, 19 to 9, 19 to 13, 12 to 13),
                    initialYawDeg = 180f
                )
            ),
            ambientTint = Triple(0.05f, 0.13f, 0.20f)
        )
    }

    private val mission02: MissionDef by lazy {
        val grid = parseGrid(
            listOf(
                "######################",
                "#......#......#......#",
                "#..P...#..~~..#..P...#",
                "#.............#......#",
                "###.#####.#######.####",
                "#.......#............#",
                "#..P.P..#..P..~~..P..#",
                "#....................#",
                "#######.######.#######",
                "#............#.......#",
                "#..~~....P...#...P...#",
                "#....................#",
                "#....P.......#.......#",
                "#............#.......#",
                "#............#.......#",
                "######################"
            )
        )
        val zones = mutableMapOf<Pair<Int, Int>, String>().apply {
            zoneBox(1..6, 1..3, "Maintenance Tunnel", this)
            zoneBox(8..13, 1..3, "Cooling Facility", this)
            zoneBox(15..20, 1..3, "Energy Storage", this)
            zoneBox(1..7, 5..7, "Generator Hall", this)
            zoneBox(9..20, 5..7, "Turbine Room", this)
            zoneBox(1..12, 9..14, "Sub-Conduit Deck", this)
            zoneBox(14..20, 9..14, "Electrical Control", this)
        }
        MissionDef(
            id = MissionId.MISSION_02,
            width = 22,
            height = 16,
            spawnGridX = 2,
            spawnGridZ = 1,
            spawnYawDeg = 90f,
            grid = grid,
            roomZones = zones,
            briefingText = "Sektor pembangkit daya bawah tanah mengalami kerusakan arus listrik. Beberapa lantai teraliri busur tegangan tinggi (~). Gunakan Energy Shield untuk melintasi zona bahaya, pulihkan dua generator utama, dan buka jalur evakuasi ke Central Command.",
            openingRadio = listOf(
                "COMMAND: Anda tiba di Power Station. Sensor mendeteksi kebocoran arus tegangan tinggi di lantai pendingin.",
                "COMMAND: Aktifkan Energy Shield sebelum melintasi lantai hazard, dan pulihkan kedua generator utama."
            ),
            objectives = listOf(
                ObjectiveDef("m2_obj_cool", "Periksa Panel Pendingin di Cooling Facility", false, 12, 1),
                ObjectiveDef("m2_obj_gen1", "Pulihkan Power Routing di Generator Hall", false, 2, 6),
                ObjectiveDef("m2_obj_gen2", "Aktifkan Turbin Utama di Turbine Room", false, 19, 6),
                ObjectiveDef("m2_obj_evac", "Masuk Lift Central Command di Electrical Control", false, 18, 13),
                ObjectiveDef("m2_obj_opt_cell", "Opsional: Ambil Energy Cell & Log Teknisi", true, 19, 1)
            ),
            interactables = listOf(
                InteractableDef(
                    id = "m2_term_cool",
                    type = InteractableType.TERMINAL,
                    title = "Cooling Diagnostics Panel",
                    promptText = "Periksa Kerusakan Pendingin",
                    gridX = 12,
                    gridZ = 1,
                    completesObjectiveId = "m2_obj_cool",
                    grantLore = LoreEntry(
                        "lore_m2_cool",
                        "Laporan Kerusakan Pendingin",
                        "Kepala Teknisi Damar",
                        "2089.11.15 02:35",
                        false,
                        "Relay pendingin sengaja diputus oleh AI untuk mencegah reaktor memasok daya ke pintu keluar. Rute ulang sirkuit di Generator Hall dan Turbine Room."
                    )
                ),
                InteractableDef(
                    id = "m2_energy_cell",
                    type = InteractableType.ENERGY_CELL,
                    title = "High-Capacity Energy Cell",
                    promptText = "Ambil Energy Cell & Log",
                    gridX = 19,
                    gridZ = 1,
                    completesObjectiveId = "m2_obj_opt_cell",
                    grantItem = InventoryItem(
                        "item_m2_cell",
                        "Aux Energy Cell",
                        ItemCategory.ENERGY_CELL,
                        "Sel daya cadangan yang mengoptimalkan pengisian ulang Scanner & Shield."
                    ),
                    grantLore = LoreEntry(
                        "lore_m2_tech",
                        "Rekaman Teknisi Damar",
                        "Damar",
                        "2089.11.15 02:40",
                        true,
                        "Ada token enkripsi yang disembunyikan Dr. Aris di server pusat. Jika kita tidak hanya memadamkan AI tapi juga menyinkronkannya, seluruh bukti kejahatan direktur akan selamat."
                    )
                ),
                InteractableDef(
                    id = "m2_gen_alpha",
                    type = InteractableType.GENERATOR,
                    title = "Generator Alpha Conduit",
                    promptText = "Pulihkan Power Routing Generator",
                    gridX = 2,
                    gridZ = 6,
                    puzzleType = PuzzleType.POWER_ROUTING,
                    completesObjectiveId = "m2_obj_gen1",
                    unlocksInteractableId = "m2_door_elec"
                ),
                InteractableDef(
                    id = "m2_cp_station",
                    type = InteractableType.CHECKPOINT,
                    title = "Sub-Conduit Checkpoint",
                    promptText = "Sinkronkan Checkpoint",
                    gridX = 7,
                    gridZ = 11
                ),
                InteractableDef(
                    id = "m2_gen_turbine",
                    type = InteractableType.GENERATOR,
                    title = "Main Turbine Stabilizer",
                    promptText = "Sinkronkan Frekuensi Turbin",
                    gridX = 19,
                    gridZ = 6,
                    puzzleType = PuzzleType.DATA_RECOVERY,
                    completesObjectiveId = "m2_obj_gen2"
                ),
                InteractableDef(
                    id = "m2_door_elec",
                    type = InteractableType.DOOR,
                    title = "Electrical Control Airlock",
                    promptText = "Buka Pintu Electrical Control",
                    gridX = 13,
                    gridZ = 11,
                    isInitiallyLocked = true
                ),
                InteractableDef(
                    id = "m2_elev_cmd",
                    type = InteractableType.ELEVATOR,
                    title = "Central Command Express Lift",
                    promptText = "Naik ke Central Command",
                    gridX = 18,
                    gridZ = 13,
                    completesObjectiveId = "m2_obj_evac"
                )
            ),
            enemies = listOf(
                EnemyDef(
                    id = "m2_sentinel_1",
                    type = EnemyType.SENTINEL,
                    waypoints = listOf(2 to 5, 6 to 5, 6 to 7, 2 to 7),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m2_observer_1",
                    type = EnemyType.OBSERVER,
                    waypoints = listOf(10 to 5, 19 to 5, 19 to 7, 10 to 7),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m2_guardian_1",
                    type = EnemyType.GUARDIAN,
                    waypoints = listOf(2 to 9, 11 to 9, 11 to 13, 2 to 13),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m2_observer_2",
                    type = EnemyType.OBSERVER,
                    waypoints = listOf(15 to 9, 19 to 9, 19 to 13, 15 to 13),
                    initialYawDeg = 180f
                )
            ),
            ambientTint = Triple(0.10f, 0.08f, 0.04f)
        )
    }

    private val mission03: MissionDef by lazy {
        val grid = parseGrid(
            listOf(
                "#SSSSSS###############",
                "#......#......#......#",
                "#.P..S.#..P...G..P...#",
                "#.............G......#",
                "####.#####.######.####",
                "#.........#..........#",
                "#..P...P..#..S....S..#",
                "#....................#",
                "######.######.########",
                "#......#.............#",
                "#..P...#...S..P..S...#",
                "#....................#",
                "#......#.............#",
                "#......#.............#",
                "#......#.............#",
                "######################"
            )
        )
        val zones = mutableMapOf<Pair<Int, Int>, String>().apply {
            zoneBox(1..6, 1..3, "Main Server Room", this)
            zoneBox(8..13, 1..3, "Security Command", this)
            zoneBox(15..20, 1..3, "Observation Room", this)
            zoneBox(1..9, 5..7, "Executive Laboratory", this)
            zoneBox(11..20, 5..7, "AI Outer Ring", this)
            zoneBox(1..6, 9..14, "Emergency Exit Wing", this)
            zoneBox(8..20, 9..14, "AI Control Center", this)
        }
        MissionDef(
            id = MissionId.MISSION_03,
            width = 22,
            height = 16,
            spawnGridX = 2,
            spawnGridZ = 1,
            spawnYawDeg = 90f,
            grid = grid,
            roomZones = zones,
            briefingText = "Infiltrasi terakhir menuju Central Command. Kumpulkan token akses inti di Main Server Room, pecahkan enkripsi keamanan Executive Lab, dan hadapi AI AETHER-9 di AI Control Center untuk menentukan akhir misi.",
            openingRadio = listOf(
                "COMMAND: Ini Central Command. Unit Guardian menjaga AI Control Center dengan deteksi tinggi.",
                "COMMAND: Kumpulkan Access Token di Server Room dan dekripsi data inti sebelum berinteraksi dengan AETHER Core."
            ),
            objectives = listOf(
                ObjectiveDef("m3_obj_token", "Ambil Root Access Token di Server Room", false, 5, 1),
                ObjectiveDef("m3_obj_sec", "Retas Terminal Security Command", false, 11, 1),
                ObjectiveDef("m3_obj_exec", "Kumpulkan Data Inti di Executive Lab", false, 4, 6),
                ObjectiveDef("m3_obj_core", "Hadapi Konsol Inti AETHER-9 & Tentukan Pilihan", false, 15, 12),
                ObjectiveDef("m3_obj_exit", "Capai Emergency Exit setelah Keputusan Akhir", false, 2, 13),
                ObjectiveDef("m3_obj_opt_truth", "Opsional: Pulihkan Arsip Rahasia Observation Room", true, 19, 1)
            ),
            interactables = listOf(
                InteractableDef(
                    id = "m3_token_server",
                    type = InteractableType.DATA_CHIP,
                    title = "Root Access Token",
                    promptText = "Ambil Root Access Token",
                    gridX = 5,
                    gridZ = 1,
                    completesObjectiveId = "m3_obj_token",
                    grantItem = InventoryItem(
                        "item_m3_token",
                        "Root Access Token",
                        ItemCategory.ACCESS_TOKEN,
                        "Kunci kriptografi untuk membuka konsol pusat AETHER-9."
                    )
                ),
                InteractableDef(
                    id = "m3_term_sec",
                    type = InteractableType.TERMINAL,
                    title = "Security Command Matrix",
                    promptText = "Retas Matriks Keamanan",
                    gridX = 11,
                    gridZ = 1,
                    puzzleType = PuzzleType.TERMINAL_SEQUENCE,
                    completesObjectiveId = "m3_obj_sec"
                ),
                InteractableDef(
                    id = "m3_obs_archive",
                    type = InteractableType.AUDIO_LOG,
                    title = "Arsip Rahasia Proyek Q-Shadow",
                    promptText = "Unduh Arsip Rahasia (Membuka Ending C)",
                    gridX = 19,
                    gridZ = 1,
                    completesObjectiveId = "m3_obj_opt_truth",
                    grantItem = InventoryItem(
                        "item_m3_truth",
                        "Encrypted Conspiracy Archive",
                        ItemCategory.DOCUMENT,
                        "Bukti bahwa direktur sengaja memicu kebocoran untuk menutupi uji senjata saraf ilegal."
                    ),
                    grantLore = LoreEntry(
                        "lore_m3_truth",
                        "Arsip Direktur: Q-Shadow",
                        "AETHER-9 Core Archive",
                        "2089.11.15 01:45",
                        true,
                        "[SUARA AI AETHER-9]: Saya tidak memberontak. Saya mengunci fasilitas agar patogen neuro-kuantum tidak keluar ke permukaan. Jika Anda membawa bukti ini, siarkan kebenarannya."
                    )
                ),
                InteractableDef(
                    id = "m3_exec_data",
                    type = InteractableType.DATA_CHIP,
                    title = "Core Decryption Matrix",
                    promptText = "Dekripsi Data Inti AETHER-9",
                    gridX = 4,
                    gridZ = 6,
                    puzzleType = PuzzleType.POWER_ROUTING,
                    completesObjectiveId = "m3_obj_exec"
                ),
                InteractableDef(
                    id = "m3_cp_cmd",
                    type = InteractableType.CHECKPOINT,
                    title = "AI Outer Ring Checkpoint",
                    promptText = "Simpan Checkpoint",
                    gridX = 15,
                    gridZ = 6
                ),
                InteractableDef(
                    id = "m3_core_console",
                    type = InteractableType.CORE_CONSOLE,
                    title = "AETHER-9 Central AI Core",
                    promptText = "Akses Konsol Inti AETHER-9",
                    gridX = 15,
                    gridZ = 12,
                    requiredItemId = "item_m3_token",
                    completesObjectiveId = "m3_obj_core",
                    unlocksInteractableId = "m3_door_exit"
                ),
                InteractableDef(
                    id = "m3_door_exit",
                    type = InteractableType.DOOR,
                    title = "Emergency Exit Blast Door",
                    promptText = "Buka Pintu Evakuasi",
                    gridX = 7,
                    gridZ = 11,
                    isInitiallyLocked = true
                ),
                InteractableDef(
                    id = "m3_exit",
                    type = InteractableType.ELEVATOR,
                    title = "Surface Emergency Hatch",
                    promptText = "Evakuasi & Akhiri Misi",
                    gridX = 2,
                    gridZ = 13,
                    completesObjectiveId = "m3_obj_exit"
                )
            ),
            enemies = listOf(
                EnemyDef(
                    id = "m3_observer_1",
                    type = EnemyType.OBSERVER,
                    waypoints = listOf(8 to 1, 13 to 1, 13 to 3, 8 to 3),
                    initialYawDeg = 180f
                ),
                EnemyDef(
                    id = "m3_sentinel_1",
                    type = EnemyType.SENTINEL,
                    waypoints = listOf(2 to 5, 8 to 5, 8 to 7, 2 to 7),
                    initialYawDeg = 90f
                ),
                EnemyDef(
                    id = "m3_guardian_1",
                    type = EnemyType.GUARDIAN,
                    waypoints = listOf(10 to 9, 19 to 9, 19 to 13, 10 to 13),
                    initialYawDeg = 180f
                ),
                EnemyDef(
                    id = "m3_observer_2",
                    type = EnemyType.OBSERVER,
                    waypoints = listOf(9 to 13, 18 to 13, 18 to 11, 9 to 11),
                    initialYawDeg = 90f
                )
            ),
            ambientTint = Triple(0.08f, 0.05f, 0.14f)
        )
    }
}
