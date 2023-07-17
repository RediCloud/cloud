package dev.redicloud.api.repositories.version

import dev.redicloud.api.repositories.java.ICloudJavaVersion
import dev.redicloud.api.repositories.java.ICloudJavaVersionRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.MINECRAFT_VERSIONS_FOLDER
import java.io.File
import java.util.concurrent.locks.ReentrantLock

interface IServerVersionHandler {

    val name: String
    val default: Boolean

    suspend fun download(version: ICloudServerVersion, force: Boolean = false): File

    suspend fun canDownload(version: ICloudServerVersion): Boolean

    fun isDownloaded(version: ICloudServerVersion): Boolean = getJar(version).exists()

    suspend fun isUpdateAvailable(version: ICloudServerVersion, force: Boolean = false): Boolean

    suspend fun getVersions(version: ICloudServerVersion): List<IServerVersion>

    suspend fun getBuilds(version: ICloudServerVersion, mcVersion: IServerVersion): List<String>

    suspend fun update(version: ICloudServerVersion, versionType: ICloudServerVersionType): File

    suspend fun patch(version: ICloudServerVersion)

    suspend fun patchCommand(type: ICloudServerVersionType, javaVersion: ICloudJavaVersion, jarToExecute: File): List<String>

    fun isPatched(version: ICloudServerVersion): Boolean =
        isPatchVersion(version) && File(getFolder(version).absolutePath, ".patched").exists()

    fun isPatchVersion(version: ICloudServerVersion): Boolean = version.patch

    fun getJar(version: ICloudServerVersion): File = File(getFolder(version).absolutePath, "${version.displayName}.jar")

    fun getFolder(version: ICloudServerVersion): File =
        File(MINECRAFT_VERSIONS_FOLDER.getFile().absolutePath, version.displayName)

    fun register() = registerHandler(this)

    fun getLock(version: ICloudServerVersion): ReentrantLock

    suspend fun shutdown(force: Boolean, serverVersionRepository: ICloudServerVersionRepository) {
        serverVersionRepository.getVersions().forEach {
            val lock = getLock(it)
            if (lock.isLocked) {
                if (!force) {
                    LOGGER.warning("Server version ${it.displayName} currently updating, waiting for it to finish...")
                    lock.lock()
                    lock.unlock()
                }else {
                    LOGGER.warning("Server version ${it.displayName} currently updating, forcing shutdown...")
                }
            }
        }
    }

    companion object {
        val LOGGER = LogManager.logger(IServerVersionHandler::class)

        val CACHE_HANDLERS = mutableListOf<IServerVersionHandler>()

        fun getHandler(type: ICloudServerVersionType): IServerVersionHandler =
            CACHE_HANDLERS.firstOrNull { it.name.lowercase() == type.versionHandlerName.lowercase() }
                ?: CACHE_HANDLERS.first { it.default }

        fun registerHandler(serverVersionHandler: IServerVersionHandler): IServerVersionHandler {
            if (CACHE_HANDLERS.any { it.name.lowercase() == serverVersionHandler.name.lowercase() })
                return CACHE_HANDLERS.first { it.name.lowercase() == serverVersionHandler.name.lowercase() }
            CACHE_HANDLERS.add(serverVersionHandler)
            return serverVersionHandler
        }

    }

}