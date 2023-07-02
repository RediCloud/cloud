package dev.redicloud.server.factory

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.utils.History
import dev.redicloud.utils.isOpen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class ServiceProcessHandler(
    private val process: Process,
    private val cloudServer: CloudServer
) {

    companion object {
        val PROCESS_SCOPE = CoroutineScope(Dispatchers.Default)
        val LOGGER = LogManager.logger(ServiceProcessHandler::class)
    }

    init {
        PROCESS_SCOPE.launch {

            val inputReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputReader)
            while (process.isAlive && inputStream.isOpen() && inputReader.ready()) {
                val line = bufferedReader.readLine()
                if (line == null || line.isEmpty()) continue
                history.add(line)
                lines.forEach { it(line) }
            }
            bufferedReader.close()
            inputReader.close()

            if (errorStream.isOpen()) {
                val errorReader = InputStreamReader(errorStream)
                val errorBufferedReader = BufferedReader(errorReader)
                while (errorStream.isOpen() && errorReader.ready()) {
                    val line = errorBufferedReader.readLine()
                    if (line == null || line.isEmpty()) continue
                    LOGGER.warning("[${cloudServer.name}]: $line")
                    history.add(line)
                    lines.forEach { it(line) }
                }
                errorBufferedReader.close()
                errorReader.close()
            }

            exits.forEach { it(process.exitValue()) }

        }
    }

    private val inputStream = process.inputStream
    private val errorStream = process.errorStream
    private val exits = mutableListOf<(Int) -> Unit>()
    private val lines = mutableListOf<(String) -> Unit>()
    val history = History<String>(100)

    suspend fun onExit(): Int = process.waitFor()

    fun onExit(block: (Int) -> Unit) {
        exits.add(block)
    }

    fun onLine(block: (String) -> Unit) {
        lines.add(block)
    }

}