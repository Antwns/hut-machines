package hut.dev.hutmachines.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Path
import java.sql.Connection
import java.time.Instant
import java.util.UUID

data class DbMachine(
    val id: UUID,
    val typeId: String,
    val world: String,
    val x: Int, val y: Int, val z: Int,
    val autoProcess: Boolean,
    val createdAt: Long = Instant.now().epochSecond
)

data class DbSlot(
    val machineId: UUID,
    val slotIndex: Int,
    val itemNbt: String?,
    val amount: Int?,
    val durability: Int?
)

class DatabaseWorker private constructor() {
    private lateinit var ds: HikariDataSource
    @Volatile private var ready = false
    var printStartupStats: Boolean = false

    companion object {
        val INSTANCE: DatabaseWorker = DatabaseWorker()
        const val SCHEMA_VERSION = 1
    }

    fun init(databaseFile: Path) {
        if (ready) return
        val jdbcUrl = "jdbc:h2:file:${databaseFile.toAbsolutePath()};MODE=MySQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE"
        val cfg = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 6
            poolName = "HutMachines-Hikari"
            connectionTimeout = 10_000
            idleTimeout = 60_000
            maxLifetime = 30 * 60_000
        }
        ds = HikariDataSource(cfg)
        ensureSchema()
        if (printStartupStats) logStats()
        ready = true
    }

    private fun ensureSchema() {
        ds.connection.use { c ->
            c.createStatement().use { st ->
                st.addBatch("""CREATE TABLE IF NOT EXISTS SCHEMA_INFO (version INT NOT NULL)""")
                st.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS MACHINES (
                        id            UUID PRIMARY KEY,
                        type_id       VARCHAR(128) NOT NULL,
                        world         VARCHAR(128) NOT NULL,
                        x             INT NOT NULL,
                        y             INT NOT NULL,
                        z             INT NOT NULL,
                        auto_process  BOOLEAN NOT NULL,
                        created_at    BIGINT NOT NULL
                    )
                    """.trimIndent()
                )
                st.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS MACHINE_SLOTS (
                        machine_id    UUID NOT NULL,
                        slot_index    INT NOT NULL,
                        item_nbt      CLOB,
                        amount        INT,
                        durability    INT,
                        PRIMARY KEY (machine_id, slot_index),
                        FOREIGN KEY (machine_id) REFERENCES MACHINES(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                st.executeBatch()
            }
            // seed version if empty
            var version: Int? = null
            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT version FROM SCHEMA_INFO")
                if (rs.next()) version = rs.getInt(1)
                rs.close()
            }
            if (version == null) {
                c.prepareStatement("INSERT INTO SCHEMA_INFO(version) VALUES (?)").use {
                    it.setInt(1, SCHEMA_VERSION); it.executeUpdate()
                }
            }
        }
    }

    private fun logStats() {
        ds.connection.use { c ->
            val machines = c.prepareStatement("SELECT COUNT(*) FROM MACHINES").use { ps ->
                ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
            }
            val items = c.prepareStatement("SELECT COALESCE(SUM(CASE WHEN amount IS NULL THEN 0 ELSE amount END),0) FROM MACHINE_SLOTS").use { ps ->
                ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
            }
            println("[HutMachines][DB] $machines machines globally; ~$items items across slots")
        }
    }

    fun upsertMachine(m: DbMachine) {
        ds.connection.use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement(
                    """
                    MERGE INTO MACHINES (id,type_id,world,x,y,z,auto_process,created_at)
                    KEY(id) VALUES (?,?,?,?,?,?,?,?)
                    """.trimIndent()
                ).use { ps ->
                    ps.setObject(1, m.id)
                    ps.setString(2, m.typeId)
                    ps.setString(3, m.world)
                    ps.setInt(4, m.x); ps.setInt(5, m.y); ps.setInt(6, m.z)
                    ps.setBoolean(7, m.autoProcess)
                    ps.setLong(8, m.createdAt)
                    ps.executeUpdate()
                }
                c.commit()
            } catch (t: Throwable) {
                c.rollback(); throw t
            } finally { c.autoCommit = true }
        }
    }

    fun replaceSlots(machineId: UUID, slots: List<DbSlot>) {
        ds.connection.use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM MACHINE_SLOTS WHERE machine_id=?").use { ps ->
                    ps.setObject(1, machineId); ps.executeUpdate()
                }
                c.prepareStatement(
                    """
                    INSERT INTO MACHINE_SLOTS (machine_id,slot_index,item_nbt,amount,durability)
                    VALUES (?,?,?,?,?)
                    """.trimIndent()
                ).use { ps ->
                    for (s in slots) {
                        ps.setObject(1, s.machineId)
                        ps.setInt(2, s.slotIndex)
                        ps.setString(3, s.itemNbt)
                        if (s.amount == null) ps.setNull(4, java.sql.Types.INTEGER) else ps.setInt(4, s.amount)
                        if (s.durability == null) ps.setNull(5, java.sql.Types.INTEGER) else ps.setInt(5, s.durability)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                c.commit()
            } catch (t: Throwable) {
                c.rollback(); throw t
            } finally { c.autoCommit = true }
        }
    }

    fun deleteMachine(machineId: UUID) {
        ds.connection.use { c ->
            c.prepareStatement("DELETE FROM MACHINES WHERE id=?").use { ps ->
                ps.setObject(1, machineId); ps.executeUpdate()
            }
        }
    }

    // --- PATCH: helpers to resolve/delete by exact block position ---

    /** Returns the machine UUID at the given world/x/y/z, or null if none. */
    fun findMachineIdAt(world: String, x: Int, y: Int, z: Int): UUID? = ds.connection.use { c ->
        c.prepareStatement(
            "SELECT id FROM MACHINES WHERE world=? AND x=? AND y=? AND z=? LIMIT 1"
        ).use { ps ->
            ps.setString(1, world); ps.setInt(2, x); ps.setInt(3, y); ps.setInt(4, z)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getObject(1, UUID::class.java) else null }
        }
    }

    /** Deletes a machine row at the given world/x/y/z. Returns true if a row was deleted. */
    fun deleteMachineAt(world: String, x: Int, y: Int, z: Int): Boolean = ds.connection.use { c ->
        c.prepareStatement(
            "DELETE FROM MACHINES WHERE world=? AND x=? AND y=? AND z=?"
        ).use { ps ->
            ps.setString(1, world); ps.setInt(2, x); ps.setInt(3, y); ps.setInt(4, z)
            ps.executeUpdate() > 0
        }
    }

    fun getMachineCount(): Long = ds.connection.use { c ->
        c.prepareStatement("SELECT COUNT(*) FROM MACHINES").use { ps ->
            ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        }
    }

    fun <T> tx(body: (Connection) -> T): T {
        ds.connection.use { c ->
            c.autoCommit = false
            return try { val r = body(c); c.commit(); r } catch (t: Throwable) { c.rollback(); throw t } finally { c.autoCommit = true }
        }
    }
}