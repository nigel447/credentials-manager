package com.krypto.manager

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.krypto.manager.components.*
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import org.apache.commons.logging.LogFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider


import tornadofx.*
import java.security.Security


object AppArtifacts {
    val appLogger = LogFactory.getLog("krypto-manager")
    val objectMapper = jacksonObjectMapper().registerModule(KotlinModule())

    var IS_LOCALSTACK = true
    val LOCALSTACK_STS_ENDPOINT = "http://localhost:9010"
    val STS_SESSION_TEST_NAME =  "role_test"

}

object AppStore {
    val SYM_KEY_PSSWD = SimpleStringProperty()
    val KEY_STORE_PASSWD = SimpleStringProperty()
    val ALIAS = SimpleStringProperty()
    val KEY_STORE = SimpleStringProperty()
    val KEY_STORE_JSON_WRAP = SimpleStringProperty()

    val ACCESS = SimpleStringProperty()
    val SECRET = SimpleStringProperty()
    val ENCRYPTED_CREDENTIALS_JSON_WRAP = SimpleStringProperty()

    val ACCOUNT_ID = SimpleStringProperty()
    val COGNITO_POOL_ID = SimpleStringProperty()
    val COGNITO_CLIENT_ID = SimpleStringProperty()
    val ENCRYPTED_COGNITO_JSON_WRAP = SimpleStringProperty()

    val ROLE_ARN = SimpleStringProperty()
    val ENCRYPTED_ACCESS = SimpleStringProperty()
    val ENCRYPTED_SECRET = SimpleStringProperty()
    val DECRYPTED_ACCESS = SimpleStringProperty()
    val DECRYPTED_SECRET = SimpleStringProperty()

    val STS_TEST_JSON_WRAP = SimpleStringProperty()


}


class Manager : App(ManagerView::class, ManagerStyle::class) {

    override fun start(stage: Stage) {
        super.start(stage)
        Security.addProvider(BouncyCastleProvider())
        val artifacts = resources.json("/artifacts.json")
        AppStore.SYM_KEY_PSSWD.set(artifacts.getString("symKePsswd"))
        AppStore.KEY_STORE_PASSWD.set(artifacts.getString("keyStorePasswd"))

    }

}


fun main(args: Array<String>) {
    launch<Manager>(args)
}


class ManagerView : View() {

    override val root = borderpane {
        top = hbox() {
            add(Header())
            HBox.setHgrow(this, Priority.ALWAYS)

        }

        center = tabpane {

            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            tab("KeyStore Generator") {
                add<KeyStoreGenerator>()
            }

            tab("Credentials Generator") {
                add<CredentialsGenerator>()
            }

            tab("Cognito Generator") {
                add<CognitoGenerator>()
            }

            tab("STS Credentials Check") {
                add<STSCredentialsCheck>()
            }

        }
    }

}


class ManagerStyle : Stylesheet() {

    val screenBounds = Screen.getPrimary().getVisualBounds()

    init {
        root {
            prefWidth = Dimension(screenBounds.width / 1.8, Dimension.LinearUnits.px)
            prefHeight = Dimension(screenBounds.height / 2, Dimension.LinearUnits.px)
        }

        appHeader {
            backgroundColor += c("#00305A")
            fontSize = 16.px
            minWidth = 100.percent

        }

        appHeaderButton {
            backgroundColor += c("forestgreen")
            textFill = Color.WHITE
            and(hover) {
                backgroundColor += c("#f0ff35")
                textFill = Color.BLACK
            }
        }

        outputText {
            fill = Color.WHITE
            textFill = c("white")
            borderColor += box(c("white"))
            borderWidth += box(3.px)
            minWidth = 100.pc
            minHeight = 100.pc
        }

        fieldset {

            legend {
                fontSize = 22.px
                fontFamily = "monospaced"
                textFill = Color.WHITE
            }
        }

        formLabel {
            fontSize = 16.px
            fontFamily = "monospaced"
            textFill = Color.WHITE
        }

        textField {
            maxHeight = 28.px
            fontSize = 14.px
        }

        submitField {
            fontFamily = "monospaced"
            fontSize = 18.px
            textFill = Color.WHITE
            minWidth = 140.px
            minHeight = 40.px

        }


        tabPane {
            borderColor += box(c("transparent"))
            backgroundColor += c("#00305A")
            // backgroundColor += c("#004B8D")

        }

        tabHeaderBackground {
            backgroundColor += c("#00305A")

        }

        tabHeaderArea {
            backgroundColor += c("#729EBF")
        }

        tabContentArea {
            backgroundColor += c("#00305A")
            borderColor += box(c("#729EBF"))
        }

        tab {
            borderColor += box(c("#729EBF"))
            borderWidth += box(3.px)
            borderRadius += box(10.0.px, 10.0.px, 0.px, 0.0.px)
            backgroundRadius += box(10.0.px, 10.0.px, 0.px, 0.0.px)
            backgroundColor += c("#00305A")

            and(selected) {
                tabLabel {
                    textFill = c("gold")
                    focusColor = c("transparent")
                    faintFocusColor = c("transparent")
                    borderColor += box(c("transparent"))

                }
                focusColor = c("transparent")
                faintFocusColor = c("transparent")
                backgroundColor += c("#729EBF")
            }

            and(focused) {
                focusColor = c("transparent")
                faintFocusColor = c("transparent")
            }

            tabLabel {
                textFill = c("white")

            }

        }
    }

    companion object {
        val formLabel by cssclass()
        val submitField by cssclass()
        val appHeader by cssclass()
        val outputText by cssclass()
        val appHeaderButton by cssclass()


    }


}