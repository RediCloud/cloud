package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.repositories.version.IServerVersionHandler

class ServerVersionHandlerSuggester : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        IServerVersionHandler.CACHE_HANDLERS.map { it.name }.toTypedArray()


}