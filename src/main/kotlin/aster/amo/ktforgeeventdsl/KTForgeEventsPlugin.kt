package aster.amo.ktforgeeventdsl

import kotlinx.coroutines.*
import net.minecraftforge.eventbus.api.Event
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.reflections.Reflections
import java.io.File
import java.util.concurrent.Executors
import java.util.logging.Logger

class ForgeEventsPlugin : Plugin<Project> {
    private val logger = Logger.getLogger(ForgeEventsPlugin::class.java.name)

    override fun apply(project: Project) {
        logger.info("Starting plugin application")

        project.tasks.register("generateForgeEvents") {
            logger.info("Registering generateForgeEvents task")
            this.doLast {
                logger.info("Executing generateForgeEvents task")
                generateEvents(project)
            }
        }

        logger.info("Plugin application completed")
    }

    private fun generateEvents(project: Project) {
        val reflections = Reflections("net.minecraftforge.event")
        val eventClasses = reflections.getSubTypesOf(Event::class.java)

        val outputDir = File(project.buildDir, "generated-src/kotlin")
        val outputFile = File(outputDir, "GeneratedForgeEvents.kt")
        outputFile.parentFile.mkdirs()

        val startTime = System.currentTimeMillis()
        val functionSignatures = runBlocking {
            val dispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()
            val signatureDeferreds = eventClasses.map { eventClass ->
                async(dispatcher) { generateFunctionSignature(eventClass) }
            }
            signatureDeferreds.awaitAll().joinToString("\n")
        }
        with(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            asCoroutineDispatcher().close()
        }


        outputFile.writeText(
            """
            package aster.amo.ktforgeeventdsl

            import net.minecraftforge.eventbus.api.SubscribeEvent
            import net.minecraftforge.fml.common.Mod
    
            @Mod.EventBusSubscriber
            object GeneratedForgeEvents {

                private val eventHandlers = mutableMapOf<Class<*>, (Any) -> Unit>()

                @Suppress("UNCHECKED_CAST")
                private fun <T : Any> registerEventHandler(eventClass: Class<T>, handler: (T) -> Unit) {
                    eventHandlers[eventClass] = handler as (Any) -> Unit
                }
        
                @SubscribeEvent
                @Suppress("UNCHECKED_CAST")
                fun onForgeEvent(event: Any) {
                    eventHandlers[event::class.java]?.invoke(event)
                }
                
                $functionSignatures
            }
            """.trimIndent()
        )


        val endTime = System.currentTimeMillis()
        println("Generated Forge Events to: ${outputFile.absolutePath} in ${endTime - startTime}ms")
    }

    private fun generateFunctionSignature(eventClass: Class<out Event>): String {
        val eventName = eventClass.simpleName
        val kotlinEventClass = eventClass.kotlin

        return if (eventClass.typeParameters.isEmpty()) {
            "fun on$eventName(handler: ($kotlinEventClass) -> Unit) = registerEventHandler($kotlinEventClass::class.java, handler)"
        } else {
            val typeParams = eventClass.typeParameters.joinToString(", ") { it.name }
            "inline fun <reified $typeParams> on$eventName(noinline handler: ($kotlinEventClass) -> Unit) = registerEventHandler($kotlinEventClass::class.java as Class<${kotlinEventClass.qualifiedName}<$typeParams>>, handler)"
        }
    }
}