package dev.redicloud.repository.template.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.service.ServiceId
import java.util.*

abstract class AbstractFileTemplateRepository(
    databaseConnection: DatabaseConnection,
    val nodeRepository: NodeRepository
) :
    DatabaseBucketRepository<FileTemplate>(databaseConnection, "file-template") {

    suspend fun getTemplate(uniqueId: UUID): FileTemplate? {
        return getHandle(uniqueId.toString()).get()
    }

    suspend fun getTemplate(displayName: String): FileTemplate? {
        return getTemplates().firstOrNull { it.getDisplayName().lowercase() == displayName.lowercase() }
    }

    suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean {
        return getHandle(uniqueId.toString()).isExists
    }

    suspend fun existsTemplate(displayName: String): Boolean {
        return getTemplates().any { it.getDisplayName().lowercase() == displayName.lowercase() }
    }

    suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun deleteTemplate(uniqueId: UUID) {
        val templates = getTemplates()
        val template =
            templates.firstOrNull { it.uniqueId == uniqueId } ?: throw Exception("Template $uniqueId not found!")
        templates.filter { it.inherited.contains(template.uniqueId) }.forEach {
            it.inherited.remove(template.uniqueId)
            updateTemplate(it)
        }
        delete(uniqueId.toString())
        if (template.getFolder().exists() && template.getFolder().isDirectory) {
            template.getFolder().deleteRecursively()
            nodeRepository.getConnectedNodes().forEach {
                pushTemplates(it.serviceId)
            }
        }
    }

    abstract suspend fun updateTemplate(template: FileTemplate): FileTemplate

    suspend fun createTemplate(template: FileTemplate): FileTemplate {
        getHandle(template.uniqueId.toString()).set(template)
        if (!template.getFolder().exists()) template.getFolder().mkdirs()
        nodeRepository.getConnectedNodes().forEach {
            pushTemplates(it.serviceId)
        }
        return template
    }

    suspend fun getTemplates(): List<FileTemplate> = getAll()

    suspend fun collectTemplates(
        vararg templates: FileTemplate
    ): List<FileTemplate> {
        val collectedTemplates = mutableListOf<FileTemplate>()
        templates.forEach { fileTemplate ->
            val template = getTemplate(fileTemplate.uniqueId)
                ?: throw Exception("Template ${fileTemplate.uniqueId} not found!")
            collectedTemplates.add(template)
            collectedTemplates.addAll(
                collectTemplates(
                    *template.inherited.mapNotNull { getTemplate(it) }.toTypedArray()
                )
            )
        }
        return collectedTemplates
    }

    abstract suspend fun pushTemplates(serviceId: ServiceId)
}