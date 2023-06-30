package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.JavaVersionSuggester
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.runBlocking

@Command("javaversion")
@CommandAlias(["jv", "javaversions"])
@CommandDescription("Manage java versions")
class JavaVersionCommand(
    private val javaVersionRepository: JavaVersionRepository,
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all java versions")
    fun list(
        actor: ConsoleActor
    ) {
        runBlocking {
            actor.sendMessage("§8<====== %hc%§nJava-Versions§8 ======§8>")
            actor.sendMessage("")
            javaVersionRepository.getVersions().forEach {
                actor.sendMessage(
                    "§8- %hc%${it.name} §8| %tc%Installed§8: ${it.isLocated(javaVersionRepository.serviceId).toSymbol()}"
                )
            }
            actor.sendMessage("")
            actor.sendMessage("§8<====== %hc%§nJava-Versions§8 ======§8>")
        }
    }

    @CommandSubPath("auto-locate")
    @CommandDescription("Auto locate all java versions")
    fun autoLocate(
        actor: ConsoleActor
    ) {
        runBlocking {
            val versions = javaVersionRepository.detectInstalledVersions()
            if (versions.isEmpty()) {
                actor.sendMessage("§cNo java versions found")
                return@runBlocking
            }
            versions.forEach {
                if (javaVersionRepository.existsVersion(it.name)) return@forEach
                javaVersionRepository.createVersion(it)
                actor.sendMessage("Created version %hc%${it.name} §8(§7${it.id}§8)")
            }
        }
    }

    @CommandSubPath("delete <version>")
    @CommandDescription("Delete a java version")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("version", true, JavaVersionSuggester::class) version: JavaVersion
    ) {
        runBlocking {
            val configTemplates = configurationTemplateRepository.getTemplates()
            if (configTemplates.any { it.javaVersionId == version.uniqueId }) {
                actor.sendMessage("§cThere are still configuration templates using this version:")
                actor.sendMessage("§c${configTemplates.filter { it.javaVersionId == version.uniqueId }.joinToString(", ") { it.name }}")
                return@runBlocking
            }
            javaVersionRepository.deleteVersion(version)
            actor.sendMessage("Deleted version ${version.name}")
        }
    }

}