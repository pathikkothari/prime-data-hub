package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.tokens.SenderUtils
import java.io.File

/**
 * Parent class of cli utilities that help our Senders.
 */
abstract class SenderUtilsCommand(
    name: String,
    help: String,
) : CliktCommand(name = name, help = help) {

    // todo this was sinfully copied from SettingsCommands.kt.  Factor out common code, and get rid of this.
    // I've tried to just extract out the stuff related to '--env' only here.
    // Note that ./prime test uses this same --env, so its usage needs to be combined with this as well.
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local environment")

    data class Environment(
        val name: String,
        val baseUrl: String,
        val useHttp: Boolean = false,
        val oktaApp: OktaCommand.OktaApp? = null
    )

    fun getEnvironment(): Environment {
        return environments.find { it.name == env } ?: abort("bad environment")
    }

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    companion object {
        val environments = listOf(
            Environment("local", "localhost:7071", useHttp = true),
            Environment("test", "test.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("staging", "staging.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("prod", "prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_PROD),
        )
    }
}

class TokenUrl : SenderUtilsCommand(
    name = "url",
    help = "Use my private key to create a URL to request an access token from ReportStream"
) {
    val privateKeyFilename by option("--private-key",
        help = "Path to private key .pem file",
        metavar = "<private-keyfile>")
        .required()

    private val scope by option(
        "--scope",
        metavar = "<desired scope>",
        help = "Specify desired authorization scope.  Example:  'report' to request access to the 'report' endpoint."
    ).required()

    private val senderName by option(
        "--sender",
        metavar = "<full name>",
        help = "Specify full name of sender, as found in settings."
    ).required()

    private fun formPath(
        environment: Environment,
        endpoint: String,
    ): String {
        val protocol = if (environment.useHttp) "http" else "https"
        return "$protocol://${environment.baseUrl}/api/$endpoint"
    }

    override fun run() {
        val environment = getEnvironment()
        val privateKeyFile = File(privateKeyFilename)
        if (! privateKeyFile.exists()) {
            echo("Unable to fine pem file " + privateKeyFile.absolutePath)
            return
        }
        val privateKey = SenderUtils.readPrivateKeyPemFile(privateKeyFile)
        if (privateKey == null) {
            echo("Unable to read private key from pem file ${privateKeyFile.absolutePath}")
            return
        }
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val sender = settings.findSender(senderName)
        if (sender == null) {
            echo("Unable to find sender full name (sender.organization) $senderName")
            return
        }
        // note:  using the sender fullName as the kid here.
        val senderToken = SenderUtils.generateSenderToken(sender, environment.baseUrl, privateKey,sender.fullName)
        val url = SenderUtils.generateSenderUrl(environment.baseUrl,senderToken)
        echo("Use this URL to get an access token from ReportStream:")
        echo(url)
     }

 }


